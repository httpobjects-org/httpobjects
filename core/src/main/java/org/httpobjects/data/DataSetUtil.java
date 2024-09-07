package org.httpobjects.data;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

public class DataSetUtil {

    public static PipedInputStream pumpDataToInputStream(Consumer<OutputStream> consumer, Executor executor){
        try{
            PipedInputStream input = new PipedInputStream();
            PipedOutputStream output = new PipedOutputStream(input);

            executor.execute(new Runnable(){
                @Override
                public void run() {
                    try{
//                        System.out.println("Copying the output");
                        consumer.accept(output);
                        output.flush();
                        output.close();
//                        System.out.println("Done copying the output");
                    }catch (Exception t){
                        t.printStackTrace();;
                    }
                }
            });
            return input;
        }catch (Throwable t){
            throw new RuntimeException(t);
        }
    }

    public static byte[] readAllBytes(Consumer<OutputStream> source){
        try{
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            source.accept(out);
            out.close();
            return out.toByteArray();
        }catch (Throwable t){
            throw new RuntimeException(t);
        }
    }

    public static byte[] readAllBytes(InputStream input, byte[] buffer) {
        try{
            final ByteArrayOutputStream out = new ByteArrayOutputStream();

            boolean keepGoing = true;
            while(keepGoing){
                final int numRead = input.read(buffer);
                System.out.println("numRead " + numRead);
                if(numRead==-1){
                    // done
                    keepGoing = false;
                }else if(numRead==0){
                    // do nothing
                }else {
                    out.write(buffer, 0, numRead);
                }
            }

            return out.toByteArray();
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }
    public static byte[] readBytes(ReadableByteChannel channel, ByteBuffer tmp, Integer limitOrNull) {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();

        copyBytes(channel, tmp, out, limitOrNull==null ? null : limitOrNull.longValue());

        return out.toByteArray();
    }

    public static byte[] readAllBytes(ReadableByteChannel channel, ByteBuffer tmp) {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();

        copyAllBytes(channel, tmp, out);

        return out.toByteArray();
    }
    public static void copyAllBytes(ReadableByteChannel channel, ByteBuffer tmp, OutputStream out) {
        copyBytes(channel, tmp, out, null);
    }

    public static void copyBytes(ReadableByteChannel channel, ByteBuffer tmp, OutputStream out, Long limitOrNull) {
        try{
            boolean keepGoing = true;
            long totalWritten = 0;
            while(keepGoing){
                final int numRead = channel.read(tmp);
                System.out.println("numRead " + numRead);
                if(numRead==-1){
                    // done
                    keepGoing = false;
                } else if(numRead==0){
                    // do nothing
                } else {
                    long totalRead = totalWritten + numRead;
                    int numWritten;

                    if(limitOrNull!=null){
                        // Limited write
                        final int overage = (int)(totalRead - limitOrNull);
                        numWritten = numRead - overage;
                        out.write(tmp.array(), 0, numWritten);
                        keepGoing = limitOrNull <= totalRead;
                    }else{
                        // Unlimited write
                        numWritten = numRead;
                        out.write(tmp.array(), 0, numRead);
                    }

                    totalWritten += numWritten;
                }
            }
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }
}
