package org.httpobjects.data;

import org.httpobjects.eventual.Eventual;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public class OutputStreamDataSource implements DataSource{
    private final Consumer<OutputStream> consumer;

    public OutputStreamDataSource(Consumer<OutputStream> consumer) {
        this.consumer = consumer;
    }

    private <T> T notSupported(){
        throw new RuntimeException("not supported");
    }

    @Override
    public Eventual<DataSession> readAsync() {
        return notSupported();
    }

    @Override
    public Eventual<InputStream> readInputStreamAsync() {
        return notSupported();
    }

    @Override
    public Eventual<ReadableByteChannel> readChannelAsync() {
        return notSupported();
    }

    @Override
    public Eventual<BigInteger> writeAsync(DataDest dest) {
        return notSupported();
    }

    @Override
    public Eventual<BigInteger> writeAsync(WritableByteChannel dest) {
        return notSupported();
    }

    @Override
    public void writeSync(OutputStream out) {
        consumer.accept(out);
    }

    @Override
    public String decodeToUTF8(int maxBytes) {
        return decodeToString(maxBytes, StandardCharsets.UTF_8);
    }

    @Override
    public String decodeToAscii(int maxBytes) {
        return decodeToString(maxBytes, StandardCharsets.US_ASCII);
    }

    @Override
    public String decodeToString(int maxBytes, Charset charset) {
        return new String(readToMemory(maxBytes), charset);
    }

    @Override
    public byte[] readToMemory(int maxBytes) {
        try{
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            writeSync(out);
            out.close();
            return out.toByteArray();
        }catch (Throwable t){
            throw new RuntimeException(t);
        }
    }
}
