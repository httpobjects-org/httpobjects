package org.httpobjects.netty4.buffer;

import java.io.*;

public class FilesystemByteAccumulatorFactory implements ByteAccumulatorFactory {
    private final File tempDir;
    private final String prefix = getClass().getSimpleName();

    public FilesystemByteAccumulatorFactory(File tempDir) {
        this.tempDir = tempDir;
    }


    class FileByteAccumulator implements ByteAccumulator{
        final File path;
        final OutputStream out;

        FileByteAccumulator(File path) {
            this.path = path;
            try {
                this.out = new FileOutputStream(path);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public InputStream toStream() {
            try {
                return new FileInputStream(path);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public OutputStream out() {
            return out;
        }
        @Override
        public void dispose() {
            if(!path.delete()) throw new RuntimeException("Unable to delete " + path.getAbsolutePath());
        }
    }
    @Override
    public ByteAccumulator newAccumulator() {
        try {
            return new FileByteAccumulator(File.createTempFile(prefix, ".body", tempDir));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
