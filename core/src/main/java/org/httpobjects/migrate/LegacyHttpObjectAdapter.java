package org.httpobjects.migrate;

import org.httpobjects.HttpObject;
import org.httpobjects.Request;
import org.httpobjects.Response;
import org.httpobjects.eventual.Eventual;

@Deprecated
public class LegacyHttpObjectAdapter extends HttpObject {
    private final LegacyHttpObject target;

    public LegacyHttpObjectAdapter(LegacyHttpObject target){
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
