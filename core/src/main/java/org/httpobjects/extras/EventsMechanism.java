package org.httpobjects.extras;

import org.httpobjects.*;
import org.httpobjects.eventual.EventualResult;
import org.httpobjects.util.HttpObjectUtil;
import org.httpobjects.util.Method;

public class EventsMechanism {

//    public interface Events<Id> {
//        Id onRequest(Request request);
//        void onResponse(Id id, Response response);
//        void onError(Throwable error);
//    }
//
//    public static <Id> HttpObject onEvents(HttpObject thiss, Events<Id> events) {
//        return eventsResource(thiss, events);
//    }
//
//    private static <Id> HttpObject eventsResource(final HttpObject resource,
//                                                  final Events<Id> events) {
//        return new SyncBridge(resource.pattern()) {
//
//            private Response dec(Method method, Request req) {
//                try {
//                    Id id = events.onRequest(req);
//                    EventualResult<Response> res = HttpObjectUtil.invokeMethod(resource, method, req);
//                    events.onResponse(id, res);
//                    return res;
//                } catch (Throwable err) {
//                    events.onError(err);
//                    throw new RuntimeException(err);
//                }
//            }
//
//            @Override
//            public Response deleteSync(Request req) {
//                return dec(Method.DELETE, req);
//            }
//
//            @Override
//            public Response getSync(Request req) {
//                return dec(Method.GET, req);
//            }
//
//            @Override
//            public Response headSync(Request req) {
//                return dec(Method.HEAD, req);
//            }
//
//            @Override
//            public Response optionsSync(Request req) {
//                return dec(Method.OPTIONS, req);
//            }
//
//            @Override
//            public Response postSync(Request req) {
//                return dec(Method.POST, req);
//            }
//
//            @Override
//            public Response putSync(Request req) {
//                return dec(Method.PUT, req);
//            }
//
//            @Override
//            public Response traceSync(Request req) {
//                return dec(Method.TRACE, req);
//            }
//
//            @Override
//            public Response patchSync(Request req) {
//                return dec(Method.PATCH, req);
//            }
//        };
//    }
}
