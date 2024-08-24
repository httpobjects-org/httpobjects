package org.httpobjects;

import org.httpobjects.eventual.Eventual;
import org.httpobjects.path.PathPattern;
import org.httpobjects.path.SimplePathPattern;

public class SyncHttpObject extends HttpObject {
    private final Response defaultResponse;

    public SyncHttpObject(PathPattern pathPattern, Response defaultResponse) {
        super(pathPattern, defaultResponse);
        this.defaultResponse = defaultResponse;
    }

    public SyncHttpObject(String pathPattern, Response defaultResponse) {
        this(new SimplePathPattern(pathPattern), defaultResponse);
    }

    public SyncHttpObject(PathPattern pathPattern) {
        this(pathPattern, METHOD_NOT_ALLOWED());
    }

    public SyncHttpObject(String pathPattern) {
        this(new SimplePathPattern(pathPattern));
    }

    public Response deleteSync(Request req){return defaultResponse;}
    public Response getSync(Request req){return defaultResponse;}
    public Response headSync(Request req){return defaultResponse;}
    public Response optionsSync(Request req){return defaultResponse;}
    public Response postSync(Request req){return defaultResponse;}
    public Response putSync(Request req){return defaultResponse;}
    public Response traceSync(Request req){return defaultResponse;}
    public Response patchSync(Request req){return defaultResponse;}


    private <T> Eventual<T> eventualIfNotNull(T r){
        return r == null ? null : Eventual.resolved(r);
    }

    @Override
    public Eventual<Response> get(Request req) {
        return eventualIfNotNull(getSync(req));
    }

    @Override
    public Eventual<Response> post(Request req) {
        return eventualIfNotNull(postSync(req));
    }

    @Override
    public Eventual<Response> put(Request req) {
        return eventualIfNotNull(putSync(req));
    }

    @Override
    public Eventual<Response> delete(Request req) {
        return eventualIfNotNull(deleteSync(req));
    }

    @Override
    public Eventual<Response> options(Request req) {
        return eventualIfNotNull(optionsSync(req));
    }

    @Override
    public Eventual<Response> head(Request req) {
        return eventualIfNotNull(headSync(req));
    }

    @Override
    public Eventual<Response> trace(Request req) {
        return eventualIfNotNull(traceSync(req));
    }

    @Override
    public Eventual<Response> patch(Request req) {
        return eventualIfNotNull(patchSync(req));
    }

}