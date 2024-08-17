package org.httpobjects.jetty;


import org.eclipse.jetty.http.HttpField;
import org.httpobjects.ConnectionInfo;
import org.httpobjects.Query;
import org.httpobjects.Representation;
import org.httpobjects.Request;
import org.httpobjects.header.HeaderField;
import org.httpobjects.header.request.RequestHeader;
import org.httpobjects.header.response.SetCookieField;
import org.httpobjects.jetty.impl.ImmutableHttpServletRequestRepresentation;
import org.httpobjects.path.Path;
import org.httpobjects.util.Method;

import java.util.*;

public class ImmutableRequestImpl implements Request {
    private final Path vars;
    private final String contentType;
    private final String query;
    private final RequestHeader header;
    private final Representation representation;
    private final ConnectionInfo connectionInfo;
    private final Method method;

    public ImmutableRequestImpl(Path vars, org.eclipse.jetty.server.Request request) {
        this.vars = vars;
        this.contentType = request.getHeaders().get("Content-Type");
        this.query = request.getHttpURI().getQuery();
        this.connectionInfo = HttpServletRequestUtil.connectionInfo(request);
        this.header = HttpServletRequestUtil.buildHeader(request);
        this.representation = ImmutableHttpServletRequestRepresentation.of(request, 0);
        this.method = Method.fromString(request.getMethod().toUpperCase());
    }

    @Override
    public Method method() {
        return method;
    }

    @Override
    public Path path() {
        return vars;
    }

    public String contentType(){
        return contentType;
    }

    @Override
    public boolean hasRepresentation() {
        return true;
    }

    @Override
    public Query query(){
        return new Query(query);
    }

    @Override
    public RequestHeader header() {
        return header;
    }

    @Override
    public Representation representation() {
        return representation;
    }

    @Override
    public Request immutableCopy() {
        return this;
    }

    @Override
    public ConnectionInfo connectionInfo() {
        return connectionInfo;
    }
}

class HttpServletRequestUtil {

    public static ConnectionInfo connectionInfo(org.eclipse.jetty.server.Request request){
        return new ConnectionInfo(
            org.eclipse.jetty.server.Request.getLocalAddr(request),
            org.eclipse.jetty.server.Request.getLocalPort(request),
            org.eclipse.jetty.server.Request.getRemoteAddr(request),
            org.eclipse.jetty.server.Request.getRemotePort(request)
        );
    }

    public static List<SetCookieField> buildCookies(org.eclipse.jetty.server.Request request) {
        final List<org.eclipse.jetty.http.HttpCookie> servletCookies = org.eclipse.jetty.server.Request.getCookies(request);
        final List<SetCookieField> cookies;
        if(servletCookies==null){
            cookies = Collections.emptyList();
        }else{
            cookies = new ArrayList<SetCookieField>(servletCookies.size());
            for(org.eclipse.jetty.http.HttpCookie next : servletCookies){
                cookies.add(HttpServletRequestUtil.buildCookie(next));
            }
        }
        return cookies;
    }

    public static SetCookieField buildCookie(org.eclipse.jetty.http.HttpCookie next) {
        return new SetCookieField(
                next.getName(),
                next.getValue(),
                next.getDomain(),
                next.getPath(),
                null,
                next.isSecure());
    }

    public static RequestHeader buildHeader(org.eclipse.jetty.server.Request request) {
        List<HeaderField> fields = new ArrayList<HeaderField>();

        for (ListIterator<HttpField> it = request.getHeaders().listIterator(); it.hasNext(); ) {
            final HttpField field = it.next();
            String name = field.getName();
            String value = field.getValue();
            fields.add(HeaderField.parse(name, value));
        }

        return new RequestHeader(fields);
    }

}
