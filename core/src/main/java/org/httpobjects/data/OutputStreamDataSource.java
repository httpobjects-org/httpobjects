package org.httpobjects.data;

import org.httpobjects.eventual.Eventual;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static org.httpobjects.data.DataSetUtil.readAllBytes;

public class OutputStreamDataSource implements DataSource{
    private static final Executor DEFAULT_THREAD_POOL = Executors.newCachedThreadPool();
    private final BiConsumer<OutputStream, Long> consumer;
    private final boolean supportsLimits;
    private final Executor executor;

    private OutputStreamDataSource(BiConsumer<OutputStream, Long> consumer, Executor executor, boolean supportsLimits) {
        this.consumer = consumer;
        this.executor = executor;
        this.supportsLimits = supportsLimits;
    }
    public OutputStreamDataSource(BiConsumer<OutputStream, Long> consumer, Executor executor) {
        this(consumer, executor,true);
    }

    public OutputStreamDataSource(BiConsumer<OutputStream, Long> consumer) {
        this(consumer, DEFAULT_THREAD_POOL);
    }

    public OutputStreamDataSource(Consumer<OutputStream> consumer, Executor executor) {
        this((out, limit)-> consumer.accept(out), executor, false);
    }


    public OutputStreamDataSource(Consumer<OutputStream> consumer) {
        this(consumer, DEFAULT_THREAD_POOL);
    }



    @Override
    public Eventual<InputStream> readInputStreamAsync() {
        return Eventual.resolved(DataSetUtil.pumpDataToInputStream(out -> consumer.accept(out, null), executor));
    }

    private Long toLongOrNull(Integer maxBytes){
        return maxBytes == null ? null : maxBytes.longValue();
    }

    @Override
    public Eventual<ReadableByteChannel> readChannelAsync() {
        return readInputStreamAsync().map(Channels::newChannel);
    }

    @Override
    public void writeSync(OutputStream out) {
        consumer.accept(out, null);
    }

    @Override
    public byte[] readToMemory(Integer maxBytes) {
        return readAllBytes(out -> {
            consumer.accept(out, toLongOrNull(maxBytes));
        });
    }

    @Override
    public boolean enforcesLimits() {
        return supportsLimits;
    }
}
