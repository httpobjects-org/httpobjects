package org.httpobjects.data;

import org.httpobjects.eventual.Eventual;

import java.io.InputStream;
import java.nio.channels.ReadableByteChannel;

class Demo {

    public void showShowingHowThisMightWork(DataSource data) throws Throwable {

        data.writeAsync(new DataDest() {
            @Override
            public void write(byte[] data, int start, int length, boolean isLast) {

            }
        });

        data.readInputStreamAsync().then(new Eventual.ResultHandler<InputStream>() {
            @Override
            public void exec(InputStream input) {
                try{
                    readStream(input);
                    input.close();
                }catch (Throwable t){
                    throw new RuntimeException(t);
                }
            }
        });

        data.readAsync().then(new Eventual.ResultHandler<DataSession>() {
            @Override
            public void exec(DataSession ds) {
//        ds.read() until return is -1
                ds.release();
            }
        });


        data.readChannelAsync().then(new Eventual.ResultHandler<ReadableByteChannel>() {
            @Override
            public void exec(ReadableByteChannel channel) {

            }
        });
    }

    public void readStream(InputStream input){}
}