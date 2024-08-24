package org.httpobjects.data;

/*
 * Non-blocking ... might return 0 bytes
 */
public interface DataSession {
    //
    int read(byte[] data, int start, int length);
    void release();
}