package org.httpobjects.data;

import org.httpobjects.eventual.Eventual;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class OutputStreamDataSource implements DataSource{
    private static final Executor DEFAULT_THREAD_POOL = Executors.newCachedThreadPool();
    private final Consumer<OutputStream> consumer;
    private final Executor executor;

    public OutputStreamDataSource(Consumer<OutputStream> consumer, Executor executor) {
        this.consumer = consumer;
        this.executor = executor;
    }

    public OutputStreamDataSource(Consumer<OutputStream> consumer) {
        this(consumer, DEFAULT_THREAD_POOL);
    }

    @Override
    public Eventual<InputStream> readInputStreamAsync() {
        try {
            return Eventual.resolved(DataSetUtil.pumpDataToInputStream(consumer, executor));
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    @Override
    public Eventual<ReadableByteChannel> readChannelAsync() {
        return readInputStreamAsync().map(Channels::newChannel);
    }

    @Override
    public void writeSync(OutputStream out) {
        consumer.accept(out);
    }

    @Override
    public byte[] readToMemory(Integer maxBytes) {
        try{
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            writeSync(out);
            out.close();
            return out.toByteArray();
        }catch (Throwable t){
            throw new RuntimeException(t);
        }
    }

    @Override
    public boolean enforcesLimits() {
        // todo - we should make it possible for implementors to take-on the burden of limit enforcement
        return false;
    }
}
