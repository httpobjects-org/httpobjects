package org.httpobjects.data;

import org.httpobjects.eventual.Eventual;

import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;

/**
 * This represents the data in a representation.  It attempts to unify the multitude of ways to provide/consume data,
 * enabling producers and consumers the option of reading/writing in the way that works best for the case at hand.
 * ---------------
 * BACKGROUND
 *
 * There are lots of different ways to provide and consume, and move data, especially along the dimensions of
 *   - thread control: who does the work?  the source?  the writer?
 *   - blocking: do read/write operations block?
 *   - async: do read/write operations return a future, finish elsewhere, etc?
 *   - streaming vs in-memory: do we need to buffer the data to RAM?
 *
 * ---------------
 * Support:
 *    - NIO/~NonBlocking
 *       - ByteChannel
 *    - NIO/Async
 *       - AsynchronousFileChannel
 *       - AsynchronousServerSocketChannel
 *       - AsynchronousSocketChannel
 *    - Push (the producer controls the thread)
 *    - Pull (the reader controls the thread)
 */
public interface DataSource {

    /* PULL MECHANISMS
     * ------------------
     * For when the consumer controls the thread
     */

    Eventual<DataSession> readAsync();
    Eventual<InputStream> readInputStreamAsync();
    Eventual<ReadableByteChannel> readChannelAsync();

    /* PULL MECHANISMS
     * ------------------
     * For when the producer controls the thread.  These return an Eventual with the total number of bytes read.
     */
    Eventual<BigInteger> writeAsync(DataDest dest);
    Eventual<BigInteger> writeAsync(WritableByteChannel dest);
    Eventual<BigInteger> writeAsync(OutputStream out);

    /* IN-MEMORY
     *  These don't scale.
     */
    Eventual<String> decodeToUTF8(long maxCharacters);
    Eventual<String> decode(long maxCharacters, Charset charset);
    Eventual<byte[]> readToMemory(int maxBytes);

}


/*
 * Then, implement default implementations for the main types of data sources
 *   - Streams (InputStream/OutputStream)
 *   -
 */
//class OutputStreamDataAccess implements DataAccess {
//    public OutputStreamDataAccess(Supplier<OutputStream> outputs){}
//}
//
//class InputStreamDataAccess implements DataAccess {
//    public InputStreamDataAccess(Supplier<InpuStream> inputs){}
//}
//
//class ByteChannelDataAccess implements DataAccess {
//    public InputStreamDataAccess(Supplier<InpuStream> inputs){}
//}

