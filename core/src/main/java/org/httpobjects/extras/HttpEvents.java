package org.httpobjects.extras;

import org.httpobjects.HttpObject;
import org.httpobjects.Request;
import org.httpobjects.Response;
import org.httpobjects.eventual.Eventual;
import org.httpobjects.util.Method;

public class HttpEvents {

    public interface HttpEventObserver<Id> {
        Id onRequest(Request request);
        void onResponse(Id id, Response response);
        void onError(Id id, Throwable error);
    }

    public static <Id> HttpObject withEventObservations(final HttpObject resource,
                                                        final HttpEventObserver<Id> eventObserver) {
        return new HttpObject(resource.pattern()) {

            private Eventual<Response> dec(Method method, Request req) {
                final Id id = eventObserver.onRequest(req);

                try{
                    Eventual<Response> e = Method.invokeMethod(resource, method, req);
                    if(e!=null){
                        e.then(res ->{
                            eventObserver.onResponse(id, res);
                        });
                    }
                    return  e;
                } catch (Throwable err) {
                    eventObserver.onError(id, err);
                    throw new RuntimeException(err);
                }
            }

            @Override
            public Eventual<Response> delete(Request req) {
                return dec(Method.DELETE, req);
            }

            @Override
            public Eventual<Response> get(Request req) {
                return dec(Method.GET, req);
            }

            @Override
            public Eventual<Response> head(Request req) {
                return dec(Method.HEAD, req);
            }

            @Override
            public Eventual<Response> options(Request req) {
                return dec(Method.OPTIONS, req);
            }

            @Override
            public Eventual<Response> post(Request req) {
                return dec(Method.POST, req);
            }

            @Override
            public Eventual<Response> put(Request req) {
                return dec(Method.PUT, req);
            }

            @Override
            public Eventual<Response> trace(Request req) {
                return dec(Method.TRACE, req);
            }

            @Override
            public Eventual<Response> patch(Request req) {
                return dec(Method.PATCH, req);
            }
        };
    }
}
