package org.httpobjects.data;

public interface DataDest {
    void write(byte[] data, int start, int length, boolean isLast);
}
