package org.httpobjects.netty4;

import org.apache.http.client.methods.HttpGet;
import org.httpobjects.*;
import org.httpobjects.eventual.Eventual;
import org.httpobjects.netty4.buffer.InMemoryByteAccumulatorFactory;
import org.httpobjects.tck.PortAllocation;
import org.httpobjects.tck.PortFinder;
import org.httpobjects.tck.WireTest;
import org.httpobjects.util.Method;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

public class Netty4ExceptionHandlingTest extends WireTest {
    PortAllocation portAllocation;
    BasicNetty4Server server;

    @Before
    public void setup() {
        portAllocation = PortFinder.allocateFreePort(this);
        server = BasicNetty4Server.serve(
                portAllocation.port,
                Arrays.asList(new HttpObject("/explode"){
                    @Override
                    public Eventual<Response> get(Request req) {
                        throw new RuntimeException("Boo!");
                    }
                }),
                ResponseCreationStrategy.synchronous(),
                new ErrorHandler(){
                    @Override
                    public Eventual<Response> createErrorResponse(HttpObject next, Method m, Throwable t) {
                        return DSL.INTERNAL_SERVER_ERROR(DSL.Text("There was an error")).resolved();
                    }
                },
                new InMemoryByteAccumulatorFactory(),
                null
        );
    }

    @Test
    public void testIt(){

        HttpGet get = new HttpGet("http://localhost:" + portAllocation.port + "/explode");

        assertResource(get, "There was an error", 500);
    }



    @After
    public void stopServing() {
        try {
            server.shutdownGracefully(5000L);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }
}
