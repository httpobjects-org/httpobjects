package org.httpobjects.representation;

import org.httpobjects.Representation;

import java.io.IOException;
import java.io.OutputStream;

public class ByteArrayRepresentation implements Representation {
    private final String contentType;
    private final byte[] data;

    public ByteArrayRepresentation(String contentType, byte[] data) {
        this.contentType = contentType;
        this.data = data;
    }

    @Override
    public String contentType() {
        return contentType;
    }

    @Override
    public void write(OutputStream out) {
        try {
            out.write(data);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Long length() {
        return (long) data.length;
    }
}
