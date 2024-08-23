package org.httpobjects;

import org.httpobjects.eventual.BasicEventualResult;
import org.httpobjects.eventual.EventualResult;
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


    @Override
    public EventualResult<Response> get(Request req) {
        return new BasicEventualResult<Response>(getSync(req));
    }

    @Override
    public EventualResult<Response> post(Request req) {
        return new BasicEventualResult<Response>(postSync(req));
    }

    @Override
    public EventualResult<Response> put(Request req) {
        return new BasicEventualResult<Response>(putSync(req));
    }

    @Override
    public EventualResult<Response> delete(Request req) {
        return new BasicEventualResult<Response>(deleteSync(req));
    }

    @Override
    public EventualResult<Response> options(Request req) {
        return new BasicEventualResult<Response>(optionsSync(req));
    }

    @Override
    public EventualResult<Response> head(Request req) {
        return new BasicEventualResult<Response>(headSync(req));
    }

    @Override
    public EventualResult<Response> trace(Request req) {
        return new BasicEventualResult<Response>(traceSync(req));
    }

    @Override
    public EventualResult<Response> patch(Request req) {
        return new BasicEventualResult<Response>(patchSync(req));
    }

}