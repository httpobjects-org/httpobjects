package org.httpobjects.extras;

import org.httpobjects.HttpObject;
import org.httpobjects.Request;
import org.httpobjects.Response;
import org.httpobjects.util.HttpObjectUtil;
import org.httpobjects.util.Method;

public class EventsMechanism {

    public interface Events<Id> {
        Id onRequest(Request request);
        void onResponse(Id id, Response response);
        void onError(Throwable error);
    }

    public static <Id> HttpObject onEvents(HttpObject thiss, Events<Id> events) {
        return eventsResource(thiss, events);
    }

    private static <Id> HttpObject eventsResource(final HttpObject resource,
                                                  final Events<Id> events) {
        return new HttpObject(resource.pattern()) {

            private Response dec(Method method, Request req) {
                try {
                    Id id = events.onRequest(req);
                    Response res = HttpObjectUtil.invokeMethod(resource, method, req);
                    events.onResponse(id, res);
                    return res;
                } catch (Throwable err) {
                    events.onError(err);
                    throw new RuntimeException(err);
                }
            }

            @Override
            public Response delete(Request req) {
                return dec(Method.DELETE, req);
            }

            @Override
            public Response get(Request req) {
                return dec(Method.GET, req);
            }

            @Override
            public Response head(Request req) {
                return dec(Method.HEAD, req);
            }

            @Override
            public Response options(Request req) {
                return dec(Method.OPTIONS, req);
            }

            @Override
            public Response post(Request req) {
                return dec(Method.POST, req);
            }

            @Override
            public Response put(Request req) {
                return dec(Method.PUT, req);
            }

            @Override
            public Response trace(Request req) {
                return dec(Method.TRACE, req);
            }

            @Override
            public Response patch(Request req) {
                return dec(Method.PATCH, req);
            }
        };
    }
}
