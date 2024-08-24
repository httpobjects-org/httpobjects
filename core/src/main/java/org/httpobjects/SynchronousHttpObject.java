package org.httpobjects;

import org.httpobjects.eventual.Eventual;
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
    public Eventual<Response> get(Request req) {
        return Eventual.resolved(target.get(req));
    }

    @Override
    public Eventual<Response> post(Request req) {
        return Eventual.resolved(target.post(req));
    }

    @Override
    public Eventual<Response> put(Request req) {
        return Eventual.resolved(target.put(req));
    }

    @Override
    public Eventual<Response> delete(Request req) {
        return Eventual.resolved(target.delete(req));
    }

    @Override
    public Eventual<Response> options(Request req) {
        return Eventual.resolved(target.options(req));
    }

    @Override
    public Eventual<Response> head(Request req) {
        return Eventual.resolved(target.head(req));
    }

    @Override
    public Eventual<Response> trace(Request req) {
        return Eventual.resolved(target.trace(req));
    }

    @Override
    public Eventual<Response> patch(Request req) {
        return Eventual.resolved(target.patch(req));
    }
}
