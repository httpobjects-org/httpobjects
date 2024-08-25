package org.httpobjects.data;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

public class DataSetUtil {

    public static PipedInputStream pumpDataToInputStream(Consumer<OutputStream> consumer, Executor executor) throws IOException {
        PipedInputStream input = new PipedInputStream();
        PipedOutputStream output = new PipedOutputStream(input);

        executor.execute(new Runnable(){
            @Override
            public void run() {
                try{
                    System.out.println("Copying the output");
                    consumer.accept(output);
                    output.flush();
                    output.close();
                    System.out.println("Done copying the output");
                }catch (Exception t){
                    t.printStackTrace();;
                }
            }
        });
        return input;
    }
}
