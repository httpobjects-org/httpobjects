package org.httpobjects.representation;

import org.httpobjects.Representation;
import org.httpobjects.data.DataSource;
import org.httpobjects.data.OutputStreamDataSource;

import java.io.IOException;

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
    public DataSource data() {
        return new OutputStreamDataSource(out -> {
            try {
                out.write(data);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }


    @Override
    public Long length() {
        return (long) data.length;
    }
}
