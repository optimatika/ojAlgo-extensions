/*
 * Copyright 1997-2023 Optimatika
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.ojalgo.rocksdb;

import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import org.ojalgo.concurrent.ProcessingService;
import org.ojalgo.function.special.PowerOf2;

final class ShardedDB<K, V> extends RocksMap<K, V> {

    private final int myIndexMask;
    private final SingleDB<K, V>[] myShards;
    private final List<SingleDB<K, V>> mySingles;

    ShardedDB(final SingleDB<K, V>[] shards, final ProcessingService executor) {

        super(executor);

        myShards = shards;
        mySingles = Arrays.asList(shards);

        int b = shards.length;
        myIndexMask = b - 1;

        if (!PowerOf2.isPowerOf2(b)) {
            throw new IllegalArgumentException("The number of shards must be a power of 2!");
        }
    }

    @Override
    public void clear() {
        this.process(mySingles, SingleDB::clear);
    }

    @Override
    public void close() {
        this.process(mySingles, SingleDB::close);
    }

    @Override
    public void compact() {
        this.process(mySingles, SingleDB::compact);
    }

    @Override
    public boolean containsKey(final Object key) {
        return myShards[key.hashCode() & myIndexMask].containsKey(key);
    }

    @Override
    public Set<Entry<K, V>> entrySet() {

        return new AbstractSet<>() {

            @Override
            public Iterator<Entry<K, V>> iterator() {

                return new Iterator<>() {

                    private int index = 0;
                    private Iterator<Entry<K, V>> shard = ShardedDB.this.getShard(index).entrySet().iterator();

                    @SuppressWarnings("resource")
                    @Override
                    public boolean hasNext() {

                        if (shard.hasNext()) {
                            return true;
                        }

                        index++;
                        if (index >= ShardedDB.this.getNumberOfShards()) {
                            return false;
                        }
                        shard = ShardedDB.this.getShard(index).entrySet().iterator();

                        if (shard.hasNext()) {
                            return true;
                        }

                        return false;
                    }

                    @Override
                    public Entry<K, V> next() {
                        return shard.next();
                    }

                };
            }

            @Override
            public int size() {
                return Integer.MAX_VALUE;
            }
        };
    }

    @Override
    public void forEach(final BiConsumer<? super K, ? super V> action) {
        this.process(mySingles, shard -> shard.forEach(action));
    }

    @Override
    public V get(final Object key) {
        return myShards[key.hashCode() & myIndexMask].get(key);
    }

    @Override
    public V put(final K key, final V value) {
        return myShards[key.hashCode() & myIndexMask].put(key, value);
    }

    @Override
    public V remove(final Object key) {
        return myShards[key.hashCode() & myIndexMask].remove(key);
    }

    @Override
    public void replaceAll(final BiFunction<? super K, ? super V, ? extends V> function) {
        this.process(mySingles, shard -> shard.replaceAll(function));
    }

    @Override
    public void switchMode(final Mode mode) {
        this.process(mySingles, shard -> shard.switchMode(mode));
    }

    int getNumberOfShards() {
        return myShards.length;
    }

    SingleDB<K, V> getShard(final int index) {
        return myShards[index];
    }

}
