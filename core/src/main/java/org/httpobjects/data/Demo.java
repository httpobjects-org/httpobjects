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

        data.readInputStreamAsync().then(input -> {
            try{
                readStream(input);
                input.close();
            }catch (Throwable t){
                throw new RuntimeException(t);
            }
        });

        data.readAsync().then(ds -> {
//        ds.read() until return is -1
            ds.release();

        });


        data.readChannelAsync().then(channel -> {

        });
    }

    public void readStream(InputStream input){}
}
