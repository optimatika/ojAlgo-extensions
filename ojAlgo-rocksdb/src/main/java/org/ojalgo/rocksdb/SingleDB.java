/*
 * Copyright 1997-2025 Optimatika
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

import java.io.File;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;

import org.ojalgo.concurrent.ProcessingService;
import org.ojalgo.netio.ToFileWriter;
import org.ojalgo.type.keyvalue.EntryPair;
import org.rocksdb.Options;
import org.rocksdb.ReadOptions;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;
import org.rocksdb.WriteBatch;
import org.rocksdb.WriteOptions;

final class SingleDB<K, V> extends RocksMap<K, V> {

    final class BatchConsumer implements ToFileWriter<EntryPair<K, V>> {

        BatchConsumer() {
            super();
        }

        @Override
        public void close() {
            // The SingleDB instance is closed elsewhere (later)
        }

        @Override
        public void write(final EntryPair<K, V> item) {
            SingleDB.this.doPut(item.getKey(), item.getValue());
        }

        @Override
        public void writeBatch(final Iterable<? extends EntryPair<K, V>> entries) {

            try (WriteBatch batch = new WriteBatch()) {

                for (EntryPair<K, V> pair : entries) {

                    K key = pair.getKey();
                    V value = pair.getValue();

                    if (value != null) {
                        batch.put(keyConverter.toBytes(key), valueConverter.toBytes(value));
                    } else {
                        batch.delete(keyConverter.toBytes(key));
                    }
                }

                SingleDB.this.writeBatch(batch);

            } catch (RocksDBException cause) {
                throw new RuntimeException(cause);
            }
        }

    }

    private ToFileWriter<EntryPair<K, V>> myBatchLoader = null;
    private final Configuration myConfiguration;
    private final File myDirectory;
    private Mode myMode;
    private transient ReadOptions myReadOptions;
    private RocksDB myRocksDB;
    private transient Options myRocksOptions;
    private transient WriteOptions myWriteOptions;

    final Converter<K> keyConverter;
    final Converter<V> valueConverter;

    SingleDB(final File dir, final Converter<K> kConverter, final Converter<V> vConverter, final Mode mode, final Configuration configuration,
            final ProcessingService executor) {

        super(executor);

        myDirectory = dir;
        myMode = mode;
        myConfiguration = configuration;

        keyConverter = kConverter;
        valueConverter = vConverter;

        this.resetOptions();

        this.newRocksDB();
    }

    @Override
    public void close() {
        if (myBatchLoader != null) {
            try {
                myBatchLoader.close();
            } catch (Exception cause) {
                throw new RuntimeException(cause);
            }
            myBatchLoader = null;
        }
        myRocksDB.close();
        this.resetOptions();
    }

    @Override
    public void compact() {
        try {
            myRocksDB.compactRange(null, null);
        } catch (RocksDBException cause) {
            throw new RuntimeException(cause);
        }
    }

    @Override
    public boolean containsKey(final Object key) {
        return this.get(key) != null;
    }

    @SuppressWarnings("resource")
    @Override
    public Set<Entry<K, V>> entrySet() {

        RocksIterator rocksIterator = myRocksDB.newIterator();
        rocksIterator.seekToFirst();
        boolean empty = !rocksIterator.isValid();

        return new AbstractSet<>() {

            @Override
            public boolean isEmpty() {
                return empty;
            }

            @Override
            public Iterator<Entry<K, V>> iterator() {
                return new Iterator<>() {

                    @Override
                    public boolean hasNext() {
                        boolean valid = rocksIterator.isValid();
                        if (!valid) {
                            rocksIterator.close();
                        }
                        return valid;
                    }

                    @Override
                    public Entry<K, V> next() {

                        K entryKey = keyConverter.toInstance(rocksIterator.key());
                        V entryValue = valueConverter.toInstance(rocksIterator.value());
                        rocksIterator.next();

                        return new Map.Entry<>() {

                            @Override
                            public K getKey() {
                                return entryKey;
                            }

                            @Override
                            public V getValue() {
                                return entryValue;
                            }

                            @Override
                            public V setValue(final V value) {
                                SingleDB.this.doPut(entryKey, value);
                                return entryValue;
                            }

                            @Override
                            public String toString() {
                                return entryKey + "=" + entryValue;
                            }
                        };
                    }
                };
            }

            @Override
            public int size() {
                return empty ? 0 : Integer.MAX_VALUE;
            }
        };

    }

    @Override
    public void forEach(final BiConsumer<? super K, ? super V> action) {

        try (RocksIterator rocksIterator = myRocksDB.newIterator()) {

            for (rocksIterator.seekToFirst(); rocksIterator.isValid(); rocksIterator.next()) {

                K entryKey = keyConverter.toInstance(rocksIterator.key());
                V entryValue = valueConverter.toInstance(rocksIterator.value());

                action.accept(entryKey, entryValue);
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public V get(final Object key) {

        Objects.requireNonNull(key);

        return this.doGet((K) key);
    }

    @Override
    public V put(final K key, final V value) {

        Objects.requireNonNull(key);
        Objects.requireNonNull(value);

        if (myBatchLoader != null) {
            myBatchLoader.write(EntryPair.of(key, value));
        } else {
            this.doPut(key, value);
        }

        return null;
    }

    @Override
    public void putAll(final Map<? extends K, ? extends V> map) {
        map.forEach(this::put);
    }

    @SuppressWarnings("unchecked")
    @Override
    public V remove(final Object key) {

        Objects.requireNonNull(key);

        if (myBatchLoader != null) {
            myBatchLoader.write(EntryPair.of((K) key, null));
        } else {
            this.doRemove((K) key);
        }

        return null;
    }

    @Override
    public void switchMode(final Mode mode) {

        Objects.requireNonNull(mode);

        if (myBatchLoader != null) {
            try {
                myBatchLoader.close();
            } catch (Exception cause) {
                throw new RuntimeException(cause);
            }
            myBatchLoader = null;
        }

        this.compact();

        myMode = mode;

        this.newRocksDB();
    }

    private ReadOptions getReadOptions() {
        if (myReadOptions == null) {
            myReadOptions = myMode.newReadOptions(myConfiguration);
        }
        return myReadOptions;
    }

    private Options getRocksOptions() {
        if (myRocksOptions == null) {
            myRocksOptions = myMode.newRocksOptions(myConfiguration);
        }
        return myRocksOptions;
    }

    private WriteOptions getWriteOptions() {
        if (myWriteOptions == null) {
            myWriteOptions = myMode.newWriteOptions(myConfiguration);
        }
        return myWriteOptions;
    }

    private void newRocksDB() {

        if (myRocksDB != null) {
            myRocksDB.close();
            myRocksDB = null;
        }

        this.resetOptions();

        try {
            ToFileWriter.mkdirs(myDirectory);
            if (myMode.isReadOnly()) {
                myRocksDB = RocksDB.openReadOnly(this.getRocksOptions(), myDirectory.getAbsolutePath());
            } else {
                myRocksDB = RocksDB.open(this.getRocksOptions(), myDirectory.getAbsolutePath());
            }
        } catch (RocksDBException cause) {
            throw new RuntimeException(cause);
        }

        if (myRocksDB == null) {
            throw new RuntimeException("The specified RocksDB could not be opened!");
        }

        if (myMode.isBulkLoad()) {
            myBatchLoader = ToFileWriter.newBuilder(myDirectory).queue(3 * myConfiguration.maxBatchSize).executor(this.getExecutor()).parallelism(1)
                    .build(t -> new BatchConsumer());
        } else {
            myBatchLoader = null;
        }
    }

    private void resetOptions() {

        if (myRocksOptions != null) {
            myRocksOptions.close();
            myRocksOptions = null;
        }

        if (myWriteOptions != null) {
            myWriteOptions.close();
            myWriteOptions = null;
        }

        if (myReadOptions != null) {
            myReadOptions.close();
            myReadOptions = null;
        }
    }

    V doGet(final K key) {

        V value = null;

        try {

            byte[] bytes = myRocksDB.get(this.getReadOptions(), keyConverter.toBytes(key));

            if (bytes != null && bytes.length > 0) {
                value = valueConverter.toInstance(bytes);
            }

        } catch (RocksDBException cause) {
            throw new RuntimeException(cause);
        }

        return value;
    }

    void doPut(final K key, final V value) {
        try {
            myRocksDB.put(this.getWriteOptions(), keyConverter.toBytes(key), valueConverter.toBytes(value));
        } catch (RocksDBException cause) {
            throw new RuntimeException(cause);
        }
    }

    void doRemove(final K key) {
        try {
            myRocksDB.delete(this.getWriteOptions(), keyConverter.toBytes(key));
        } catch (RocksDBException cause) {
            throw new RuntimeException(cause);
        }
    }

    void writeBatch(final WriteBatch batch) {
        try {
            myRocksDB.write(this.getWriteOptions(), batch);
        } catch (RocksDBException cause) {
            throw new RuntimeException(cause);
        }
    }

}
