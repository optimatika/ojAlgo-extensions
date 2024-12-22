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

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import org.ojalgo.concurrent.Parallelism;
import org.ojalgo.concurrent.ProcessingService;
import org.ojalgo.function.special.PowerOf2;
import org.ojalgo.netio.ShardedFile;
import org.rocksdb.CompressionType;
import org.rocksdb.Options;
import org.rocksdb.ReadOptions;
import org.rocksdb.RocksDB;
import org.rocksdb.WriteOptions;

/**
 * <P>
 * A {@link RocksDB} based {@link Map} implementation.
 * <P>
 * Largely functions as a normal {@link Map}, but there are a few differences. For instance {@link #size()}
 * and {@link #isEmpty()} always return the same dummy values (maximum size and not empty). Further the
 * {@link #put(Object, Object)} and {@link #remove(Object)} methods always return <code>null</code>.
 * <P>
 * <code>null</code> keys and/or values are not permitted.
 * <P>
 * Particular attention has been given to maximise bulk loading performance. To make use of this, specify that
 * you are going to bulk load, and use multiple shards:
 * <ol>
 * <li>{@link RocksMap.Builder#mode(Mode)}</li>
 * <li>{@link RocksMap.Mode#LOAD}</li>
 * <li>{@link RocksMap.Builder#shards(int)}</li>
 * </ol>
 */
public abstract class RocksMap<K, V> extends AbstractMap<K, V> implements AutoCloseable {

    public static final class Builder<K, V> {

        private final Configuration myConfiguration;
        private final File myDirectory;
        private ExecutorService myExecutor = null;
        private final Converter<K> myKeyConverter;
        private Mode myMode = Mode.MIXED;
        private transient ProcessingService myProcessor = null;
        private final Converter<V> myValueConverter;

        <KK, VV> Builder(final Builder<KK, VV> template, final Converter<K> kConverter, final Converter<V> vConverter) {

            super();

            myDirectory = template.getDirectory();
            myConfiguration = template.getConfiguration();

            myKeyConverter = kConverter;
            myValueConverter = vConverter;
        }

        Builder(final File dir, final Converter<K> kConverter, final Converter<V> vConverter) {

            super();

            myDirectory = dir;
            myConfiguration = new Configuration();

            myKeyConverter = kConverter;
            myValueConverter = vConverter;
        }

        public RocksMap<K, V> build() {
            if (myConfiguration.numberOfShards > 1) {
                return this.buildSharded();
            } else {
                return this.buildSingle();
            }
        }

        public Builder<K, V> compression(final CompressionType type) {
            myConfiguration.compression = type;
            return this;
        }

        public Builder<K, V> executor(final ExecutorService executor) {
            myExecutor = executor;
            myProcessor = null;
            return this;
        }

        public <KK> Builder<KK, V> key(final Converter<KK> kConverter) {
            Objects.requireNonNull(kConverter);
            return new Builder<>(this, kConverter, myValueConverter);
        }

        public <KK> Builder<KK, V> key(final Function<KK, byte[]> toBytesFunction, final Function<byte[], KK> toInstanceFunction) {
            Objects.requireNonNull(toBytesFunction);
            Objects.requireNonNull(toInstanceFunction);
            return this.key(new ComposedConverter<>(toBytesFunction, toInstanceFunction));
        }

        /**
         * When bulk loading key-value pairs are put in batches to the underlying {@link RocksDB} instance –
         * what's the max batch size
         */
        public Builder<K, V> maxBatchSize(final int value) {
            myConfiguration.maxBatchSize = value;
            return this;
        }

        /**
         * Default for RocksDB is -1 which means "infinite" – that can be problematic in some situations.
         */
        public Builder<K, V> maxOpenFiles(final int value) {
            myConfiguration.maxOpenFiles = value;
            return this;
        }

        public Builder<K, V> mode(final Mode mode) {
            Objects.requireNonNull(mode);
            myMode = mode;
            return this;
        }

        /**
         * If using {@link Map#entrySet()}, {@link Map#forEach(BiConsumer)} or any other method that requires
         * iterating through the entire database, it is probably better to set this to <code>false</code>.
         */
        public Builder<K, V> pointLookup(final boolean flag) {
            myConfiguration.pointLookup = flag;
            return this;
        }

        /**
         * https://github.com/facebook/rocksdb/wiki/Block-Cache
         *
         * @param blockCacheSize MB
         */
        public Builder<K, V> pointLookup(final long blockCacheSize) {
            myConfiguration.pointLookup = true;
            myConfiguration.blockCacheSizeInMB = blockCacheSize;
            return this;
        }

        /**
         * For best performance one should not have too many threads writing to the same {@link RocksDB}
         * instance. With high level of concurreny it is better to have a set of {@link RocksDB}
         * instances/shards used in parallel. This is what you define here.
         */
        public Builder<K, V> shards(final int nbShards) {
            myConfiguration.numberOfShards = PowerOf2.adjustUp(nbShards);
            return this;
        }

        public <VV> Builder<K, VV> value(final Converter<VV> vConverter) {
            Objects.requireNonNull(vConverter);
            return new Builder<>(this, myKeyConverter, vConverter);
        }

        public <VV> Builder<K, VV> value(final Function<VV, byte[]> toBytesFunction, final Function<byte[], VV> toInstanceFunction) {
            Objects.requireNonNull(toBytesFunction);
            Objects.requireNonNull(toInstanceFunction);
            return this.value(new ComposedConverter<>(toBytesFunction, toInstanceFunction));
        }

        private RocksMap<K, V> buildSharded() {

            int nbShards = myConfiguration.numberOfShards;

            ShardedFile shardsInfo = ShardedFile.of(new File(myDirectory, "shard"), nbShards);
            @SuppressWarnings("unchecked")
            SingleDB<K, V>[] singles = (SingleDB<K, V>[]) new SingleDB<?, ?>[nbShards];
            for (int i = 0; i < nbShards; i++) {
                singles[i] = new SingleDB<>(shardsInfo.shard(i), myKeyConverter, myValueConverter, myMode, myConfiguration, this.getProcessor());
            }

            return new ShardedDB<>(singles, this.getProcessor());
        }

        private RocksMap<K, V> buildSingle() {
            return new SingleDB<>(myDirectory, myKeyConverter, myValueConverter, myMode, myConfiguration, this.getProcessor());
        }

        Configuration getConfiguration() {
            return myConfiguration;
        }

        File getDirectory() {
            return myDirectory;
        }

        Mode getMode() {
            return myMode;
        }

        ProcessingService getProcessor() {
            if (myProcessor == null) {
                if (myExecutor != null) {
                    myProcessor = new ProcessingService(myExecutor);
                } else {
                    myProcessor = ProcessingService.newInstance("RocksMap-" + myDirectory.getName());
                }
            }
            return myProcessor;
        }

    }

    public interface Converter<T> {

        byte[] toBytes(T instance);

        T toInstance(byte[] bytes);

    }

    public enum Mode {

        /**
         * Write-only, bulk loading
         * <p>
         * If you use this, then {@link RocksMap#switchMode(Mode)} before accessing data.
         *
         * @see https://github.com/facebook/rocksdb/wiki/RocksDB-FAQ
         */
        LOAD(true, false),
        /**
         * Read-only
         */
        LOOKUP(false, true),
        /**
         * Normal usage, mixed reading and writing
         */
        MIXED(false, false);

        private final boolean myBulkLoad;
        private final boolean myReadOnly;

        Mode(final boolean bulk, final boolean readOnly) {
            myBulkLoad = bulk;
            myReadOnly = readOnly;
        }

        public boolean isBulkLoad() {
            return myBulkLoad;
        }

        public boolean isReadOnly() {
            return myReadOnly;
        }

        ReadOptions newReadOptions(final Configuration configuration) {
            return new ReadOptions();
        }

        Options newRocksOptions(final Configuration configuration) {

            Options options = new Options();

            if (configuration.pointLookup) {
                options = options.optimizeForPointLookup(configuration.blockCacheSizeInMB);
            }

            if (this.isBulkLoad()) {
                options = options.prepareForBulkLoad();
            }

            options.setCreateIfMissing(true);

            if (configuration.maxOpenFiles > 0) {
                options.setMaxOpenFiles(configuration.maxOpenFiles);
            }

            if (configuration.compression != null) {
                options.setCompressionType(configuration.compression);
            }

            int parallelism = Math.min(Math.max(2, configuration.numberOfShards), Parallelism.CORES.getAsInt());
            options.setMaxBackgroundJobs(parallelism);
            options.setMaxSubcompactions(parallelism);

            return options;
        }

        WriteOptions newWriteOptions(final Configuration configuration) {

            WriteOptions options = new WriteOptions();

            options.setDisableWAL(true);
            options.setSync(false);

            return options;
        }
    }

    static final class ComposedConverter<T> implements Converter<T> {

        private final Function<T, byte[]> myToBytesFunction;
        private final Function<byte[], T> myToInstanceFunction;

        ComposedConverter(final Function<T, byte[]> toBytesFunction, final Function<byte[], T> toInstanceFunction) {
            super();
            myToBytesFunction = toBytesFunction;
            myToInstanceFunction = toInstanceFunction;
        }

        @Override
        public byte[] toBytes(final T instance) {
            return myToBytesFunction.apply(instance);
        }

        @Override
        public T toInstance(final byte[] bytes) {
            return myToInstanceFunction.apply(bytes);
        }

    }

    static final class Configuration {

        long blockCacheSizeInMB = 64;
        CompressionType compression = null;
        int maxBatchSize = 2048;
        int maxOpenFiles = -1;
        int numberOfShards = 1;
        boolean pointLookup = true;

    }

    static final Converter<String> STRING_CONVERTER = new Converter<>() {

        @Override
        public byte[] toBytes(final String instance) {
            return instance.getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public String toInstance(final byte[] bytes) {
            return new String(bytes, StandardCharsets.UTF_8);
        }
    };

    static {
        RocksDB.loadLibrary();
    }

    /**
     * Returns a {@link RocksMap.Builder} instance to configure the {@link RocksMap}. The default type, for
     * both the keys and values, is {@link String}. You change the types by specifying the converters using
     * {@link Builder#key(Converter)} and {@link Builder#value(Converter)}.
     */
    public static Builder<String, String> newBuilder(final File rocksDir) {
        return new Builder<>(rocksDir, STRING_CONVERTER, RocksMap.STRING_CONVERTER);
    }

    private final ProcessingService myProcessor;

    RocksMap(final ProcessingService processor) {

        super();

        myProcessor = processor;
    }

    public abstract void compact();

    /**
     * Always return <code>false</code>.
     *
     * @see java.util.AbstractMap#isEmpty()
     */
    @Override
    public final boolean isEmpty() {
        return false;
    }

    /**
     * Always returns {@link Integer#MAX_VALUE}.
     *
     * @see java.util.AbstractMap#size()
     */
    @Override
    public final int size() {
        return Integer.MAX_VALUE;
    }

    /**
     * Switch usage mode. Will change various options. If necessary (it most likely is) the underlying
     * {@link RocksDB} instance will be closed and reopened.
     * <p>
     * In particular this is important when swiching to/from {@link Mode#LOAD}.
     */
    public abstract void switchMode(Mode mode);

    final ExecutorService getExecutor() {
        return myProcessor.getExecutor();
    }

    final <W> void process(final List<W> work, final Consumer<W> processor) {
        myProcessor.process(work, processor);
    }

}
