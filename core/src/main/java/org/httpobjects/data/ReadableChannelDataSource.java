package org.httpobjects.data;

import org.httpobjects.eventual.Eventual;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.function.Supplier;

public class ReadableChannelDataSource implements DataSource{
    private final Supplier<ReadableByteChannel> supplier;

    public ReadableChannelDataSource(Supplier<ReadableByteChannel> supplier) {
        this.supplier = supplier;
    }

    @Override
    public Eventual<InputStream> readInputStreamAsync() {
        return Eventual.resolved(Channels.newInputStream(supplier.get()));
    }

    @Override
    public Eventual<ReadableByteChannel> readChannelAsync() {
        return Eventual.resolved(supplier.get());
    }

    // TODO: expose the buffer or buffer size so that callers can tune this
    private ByteBuffer allocateBuffer(){
        return ByteBuffer.allocate(1024 * 1024);
    }

    @Override
    public void writeSync(OutputStream out) {
        DataSetUtil.copyAllBytes(supplier.get(), allocateBuffer(), out);
    }

    @Override
    public byte[] readToMemory(Integer maxBytes) {
        return DataSetUtil.readBytes(supplier.get(), allocateBuffer(), maxBytes);
    }

    @Override
    public boolean enforcesLimits() {
        return true;
    }
}
