package org.httpobjects.extras;

import org.httpobjects.*;
import org.httpobjects.eventual.Eventual;
import org.httpobjects.header.request.RequestHeader;
import org.httpobjects.path.Path;
import org.httpobjects.util.Method;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.httpobjects.DSL.allowed;
import static org.junit.Assert.*;

public class HttpEventsTest {

    @Test
    public void decorateShouldApplyDecoratorOnEvents() throws Exception {
        // given
        SampleHttpEventObserver observer = new SampleHttpEventObserver();
        HttpObject resource = new HttpObject("/", allowed(Method.GET, Method.POST)) {
            @Override public Eventual<Response> get(Request req) { return OK(Text("You Got!")).resolved(); }
            @Override public Eventual<Response> post(Request req) { return OK(Text("You Post!")).resolved(); }
        };
        HttpObject decorated = HttpEvents.withEventObservations(resource, observer);
        Request getReq = request("/", Method.GET);
        Request postReq = request("/", Method.POST);

        // when
        Response getRes = decorated.get(getReq).join();
        Response postRes = decorated.post(postReq).join();

        // then
        assertEquals("Request: 1, " + getReq, observer.log.get(0));
        assertEquals("Response: 1, " + getRes, observer.log.get(1));
        assertEquals("Request: 2, " + postReq, observer.log.get(2));
        assertEquals("Response: 2, " + postRes, observer.log.get(3));
    }

    @Test
    public void decorateShouldRethrowErrors() throws Exception {
        // given
        SampleHttpEventObserver observer = new SampleHttpEventObserver();
        RuntimeException error = new RuntimeException();
        HttpObject resource = new HttpObject("/", allowed(Method.GET)) {
            @Override public Eventual<Response> get(Request req) { throw error; }
        };
        HttpObject decorated = HttpEvents.withEventObservations(resource, observer);
        Object successResult;
        Throwable errorResult;

        // when
        try {
            successResult = decorated.get(request("/", Method.GET));
            errorResult = null;
        }
        catch (Throwable err) {
            successResult = null;
            errorResult = err;
        }

        // then
        assertNull(successResult);
        assertNotNull(errorResult);
        assertEquals(error, errorResult.getCause());
    }


    private static class SampleHttpEventObserver implements HttpEvents.HttpEventObserver<Integer> {

        AtomicInteger atomicEventId = new AtomicInteger();

        List<String> log = new ArrayList<>();

        @Override
        public Integer onRequest(Request request) {
            Integer eventId = atomicEventId.incrementAndGet();
            log.add("Request: " + eventId + ", " + request);
            return eventId;
        }

        @Override
        public void onResponse(Integer eventId, Response response) {
            log.add("Response: " + eventId + ", " + response);
        }

        @Override
        public void onError(Throwable error) {
            log.add("Error: " + error);
        }
    }

    private Request request(String path, Method method) {
        return new Request() {
            @Override public Query query() { return new Query(""); }
            @Override public Path path() { return new Path(path); }
            @Override public RequestHeader header() { return new RequestHeader(); }
            @Override public ConnectionInfo connectionInfo() {
                return new ConnectionInfo("10.10.10.10", 40,
                        "20.20.20.20", 80);
            }
            @Override public boolean hasRepresentation() { return false; }
            @Override public Representation representation() { return null; }
            @Override public Request immutableCopy() { return this; }
            @Override public Method method() { return method; }
        };
    }
}
