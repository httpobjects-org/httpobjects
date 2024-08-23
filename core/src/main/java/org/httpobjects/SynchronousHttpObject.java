package org.httpobjects;

import org.httpobjects.eventual.BasicEventualResult;
import org.httpobjects.eventual.EventualResult;
import org.httpobjects.path.PathPattern;
import org.httpobjects.path.SimplePathPattern;

public class SynchronousHttpObject extends DSL{

    private final PathPattern pathPattern;
    private final Response defaultResponse;

    public SynchronousHttpObject(PathPattern pathPattern, Response defaultResponse) {
        super();
        this.pathPattern = pathPattern;
        this.defaultResponse = defaultResponse;
    }

    public SynchronousHttpObject(String pathPattern, Response defaultResponse) {
        this(new SimplePathPattern(pathPattern), defaultResponse);
    }

    public SynchronousHttpObject(PathPattern pathPattern) {
        this(pathPattern, METHOD_NOT_ALLOWED());
    }

    public SynchronousHttpObject(String pathPattern) {
        this(new SimplePathPattern(pathPattern));
    }

    public PathPattern pattern() {
        return pathPattern;
    }


    public Response delete(Request req){return defaultResponse;}
    public Response get(Request req){return defaultResponse;}
    public Response head(Request req){return defaultResponse;}
    public Response options(Request req){return defaultResponse;}
    public Response post(Request req){return defaultResponse;}
    public Response put(Request req){return defaultResponse;}
    public Response trace(Request req){return defaultResponse;}
    public Response patch(Request req){return defaultResponse;}


    public HttpObject async(){
        return new SyncWrapper(this);
    }
}

class SyncWrapper extends HttpObject {
    private final SynchronousHttpObject target;

    public SyncWrapper(SynchronousHttpObject target){
        super(target.pattern());
        this.target = target;
    }

    @Override
    public EventualResult<Response> get(Request req) {
        return new BasicEventualResult<Response>(target.get(req));
    }

    @Override
    public EventualResult<Response> post(Request req) {
        return new BasicEventualResult<Response>(target.post(req));
    }

    @Override
    public EventualResult<Response> put(Request req) {
        return new BasicEventualResult<Response>(target.put(req));
    }

    @Override
    public EventualResult<Response> delete(Request req) {
        return new BasicEventualResult<Response>(target.delete(req));
    }

    @Override
    public EventualResult<Response> options(Request req) {
        return new BasicEventualResult<Response>(target.options(req));
    }

    @Override
    public EventualResult<Response> head(Request req) {
        return new BasicEventualResult<Response>(target.head(req));
    }

    @Override
    public EventualResult<Response> trace(Request req) {
        return new BasicEventualResult<Response>(target.trace(req));
    }

    @Override
    public EventualResult<Response> patch(Request req) {
        return new BasicEventualResult<Response>(target.patch(req));
    }
}
