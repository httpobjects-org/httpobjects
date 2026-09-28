package org.httpobjects.netty4;

import org.httpobjects.HttpObject;
import org.httpobjects.Request;
import org.httpobjects.Response;
import org.httpobjects.eventual.Eventual;
import org.httpobjects.netty4.buffer.InMemoryByteAccumulatorFactory;
import org.httpobjects.tck.PortAllocation;
import org.httpobjects.tck.PortFinder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertEquals;

/**
 * Reproduces the HTTP-pipelining request race in {@link HttpobjectsChannelHandler}.
 *
 * <p>When two requests are pipelined on a single keep-alive connection, the Netty I/O
 * thread reads the second request -- reassigning the handler's {@code currentRequest}
 * field -- before the asynchronous response {@code Runnable} for the first request runs.
 * Because that Runnable closed over the instance field instead of a local copy, it read
 * the wrong request's accumulator: request one's handler processed request two's body
 * and disposed request two's buffer.
 *
 * <p>The race is otherwise timing-dependent. {@link GatedResponseExecutor} makes it
 * deterministic: it withholds both response Runnables until both pipelined requests have
 * been fully read, by which point {@code currentRequest} definitely points at request
 * two. It then runs the Runnables one at a time, which also keeps response writes from
 * interleaving -- isolating this test from the separate, unfixed concern of response
 * <em>ordering</em> under pipelining (the assertion below is order-independent for the
 * same reason).
 */
public class Netty4PipeliningTest {
    private PortAllocation portAllocation;
    private BasicNetty4Server server;
    private GatedResponseExecutor responseExecutor;

    @Before
    public void setup() {
        portAllocation = PortFinder.allocateFreePort(this);
        responseExecutor = new GatedResponseExecutor();
        server = BasicNetty4Server.serveHttp(
                portAllocation.port,
                Arrays.asList(
                        new HttpObject("/one") {
                            @Override
                            public Eventual<Response> get(Request req) {
                                return OK(Text("response-one")).resolved();
                            }
                        },
                        new HttpObject("/two") {
                            @Override
                            public Eventual<Response> get(Request req) {
                                return OK(Text("response-two")).resolved();
                            }
                        }),
                ResponseCreationStrategy.async(responseExecutor),
                new InMemoryByteAccumulatorFactory());
    }

    @After
    public void stopServing() {
        try {
            server.shutdownGracefully(5000L);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        } finally {
            responseExecutor.shutdown();
        }
    }

    @Test
    public void pipelinedRequestsEachGetTheirOwnResponse() throws Exception {
        final String requests =
                "GET /one HTTP/1.1\r\nHost: localhost\r\n\r\n" +
                "GET /two HTTP/1.1\r\nHost: localhost\r\n\r\n";

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", portAllocation.port), 5000);
            socket.setTcpNoDelay(true);
            socket.setSoTimeout(5000);

            // Send both requests in a single write so the server buffers them together,
            // the way a pipelining client would.
            final OutputStream out = socket.getOutputStream();
            out.write(requests.getBytes(StandardCharsets.US_ASCII));
            out.flush();

            final InputStream in = socket.getInputStream();
            final List<String> bodies = Arrays.asList(
                    readOneResponseBody(in),
                    readOneResponseBody(in));

            // Each pipelined request must be answered by its own resource. With the bug,
            // both responses are "response-two". Sorted so the assertion does not depend
            // on response ordering.
            final List<String> sorted = new ArrayList<>(bodies);
            Collections.sort(sorted);
            assertEquals(Arrays.asList("response-one", "response-two"), sorted);
        }
    }

    /**
     * Withholds response Runnables until both pipelined requests have been submitted, then
     * runs them sequentially. {@code execute} is only ever called from the single Netty
     * I/O thread, so no synchronization of {@link #pending} is needed.
     */
    private static final class GatedResponseExecutor implements Executor {
        private final ExecutorService delegate = Executors.newSingleThreadExecutor();
        private final List<Runnable> pending = new ArrayList<>();

        @Override
        public void execute(Runnable command) {
            pending.add(command);
            if (pending.size() == 2) {
                // Both LastHttpContent messages have now been read, so the I/O thread has
                // already reassigned currentRequest to request two. Releasing here is what
                // makes the race deterministic.
                for (Runnable r : pending) {
                    delegate.execute(r);
                }
            }
        }

        void shutdown() {
            delegate.shutdownNow();
        }
    }

    /** Reads a single Content-Length-delimited HTTP/1.1 response and returns its body. */
    private static String readOneResponseBody(InputStream in) throws IOException {
        final byte[] headerBytes = readUntilBlankLine(in);
        final String headers = new String(headerBytes, StandardCharsets.US_ASCII);

        final int contentLength = contentLengthOf(headers);
        final byte[] body = new byte[contentLength];
        int read = 0;
        while (read < contentLength) {
            final int n = in.read(body, read, contentLength - read);
            if (n == -1) {
                throw new EOFException("Connection closed before the response body was complete");
            }
            read += n;
        }
        return new String(body, StandardCharsets.UTF_8);
    }

    /** Reads bytes up to and including the CRLFCRLF that terminates the response headers. */
    private static byte[] readUntilBlankLine(InputStream in) throws IOException {
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        final int[] last = {0, 0, 0, 0};
        while (true) {
            final int b = in.read();
            if (b == -1) {
                throw new EOFException("Connection closed before the response headers were complete");
            }
            buffer.write(b);
            last[0] = last[1];
            last[1] = last[2];
            last[2] = last[3];
            last[3] = b;
            if (last[0] == '\r' && last[1] == '\n' && last[2] == '\r' && last[3] == '\n') {
                return buffer.toByteArray();
            }
        }
    }

    private static int contentLengthOf(String headers) {
        for (String line : headers.split("\r\n")) {
            final int colon = line.indexOf(':');
            if (colon > 0 && line.substring(0, colon).trim().equalsIgnoreCase("Content-Length")) {
                return Integer.parseInt(line.substring(colon + 1).trim());
            }
        }
        throw new IllegalStateException("No Content-Length header in response:\n" + headers);
    }
}
