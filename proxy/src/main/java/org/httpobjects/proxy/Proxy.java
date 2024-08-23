/**
 * Copyright (C) 2011, 2012 Commission Junction Inc.
 *
 * This file is part of httpobjects.
 *
 * httpobjects is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * httpobjects is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with httpobjects; see the file COPYING.  If not, write to the
 * Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole
 * combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent
 * modules, and to copy and distribute the resulting executable under
 * terms of your choice, provided that you also meet, for each linked
 * independent module, the terms and conditions of the license of that
 * module.  An independent module is a module which is not derived from
 * or based on this library.  If you modify this library, you may extend
 * this exception to your version of the library, but you are not
 * obligated to do so.  If you do not wish to do so, delete this
 * exception statement from your version.
 */
package org.httpobjects.proxy;

import org.httpobjects.*;
import org.httpobjects.client.HttpClient;
import org.httpobjects.header.HeaderField;
import org.httpobjects.header.response.LocationField;
import org.httpobjects.header.response.SetCookieField;
import org.httpobjects.impl.HTLog;
import org.httpobjects.path.PathPattern;
import org.httpobjects.path.RegexPathPattern;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class Proxy extends HttpObject {
    private final HTLog log = new HTLog(this);
    private String base;
    private final String me;
    private final HttpClient client;

    public Proxy(final String localPath, final String base, final String me, final HttpClient client) {
        super(
        		makePathPattern(localPath), 
        		null);
        setBase(base);
        this.me = me;
        this.client = client;
    }

    public Proxy(final String base, final String me, final HttpClient client) {
        this("", base, me, client);
    }
    
    private static PathPattern makePathPattern(String localPath){
    		return new RegexPathPattern(Pattern.compile(Pattern.quote(localPath) + "/?(.*)"), "path");
    }

    public void setBase(String base) {
        this.base = stripTrailingSlash(base);
    }

    public String getBase() {
        return base;
    }

    @Override
    public Response get(Request req) {
        return prepRequest(req).execute("get");
    }

    @Override
    public Response delete(Request req) {
        return prepRequest(req).execute("delete");
    }

    @Override
    public Response put(Request req) {
        return prepRequest(req).execute("put");
    }

    @Override
    public Response patch(Request req) {
        return prepRequest(req).execute("patch");
    }

    @Override
    public Response options(Request req) {
        return prepRequest(req).execute("options");
    }

    @Override
    public Response post(Request req) {
        return prepRequest(req).execute("post");
    }

    protected String getQuery(Request req) {
        return req.query().toString();
    }

    class PreparedRequest{
        final String url;
        final Representation representation;
        final String query;
        final HeaderField[] headerFields;
        final HttpClient.RemoteObject object;

        PreparedRequest(String url, Representation representation, String query, HeaderField[] headerFields, HttpClient.RemoteObject object) {
            this.url = url;
            this.representation = representation;
            this.query = query;
            this.headerFields = headerFields;
            this.object = object;
        }

        Response execute(String method){
            log.info("Executing " + method + " " + url);
            final Response r;
            final PreparedRequest prep = this;
            if(method.equalsIgnoreCase("get")){
                r = prep.object.get(prep.representation, prep.query, prep.headerFields);
            } else if(method.equalsIgnoreCase("delete")){
                r = prep.object.delete(prep.representation, prep.query, prep.headerFields);
            } else if(method.equalsIgnoreCase("put")){
                r = prep.object.put(prep.representation, prep.query, prep.headerFields);
            } else if(method.equalsIgnoreCase("patch")){
                r = prep.object.patch(prep.representation, prep.query, prep.headerFields);
            } else if(method.equalsIgnoreCase("options")){
                r = prep.object.options(prep.representation, prep.query, prep.headerFields);
            } else if(method.equalsIgnoreCase("post")){
                r = prep.object.post(prep.representation, prep.query, prep.headerFields);
            } else {
                throw new RuntimeException("Not a method I support: " + method);
            }

            log.info("Immediate response: " + r.code().name());

            return handleResponse(r);
        }
    }

    private PreparedRequest prepRequest(Request req) {

        String path = req.path().valueFor("path");
        if(path == null) path = "";
        if (!path.startsWith("/")) path = "/" + path;
        String query = getQuery(req);
        if (query == null || query.equals("?")) {
            query = "";
        }
        String url = base + path;

        List<HeaderField> headers = new ArrayList<HeaderField>();

        for (HeaderField next : req.header().fields()) {
            headers.add(HeaderField.parse(next.name(), next.value()));
        }

        if (req.representation().contentType() != null) {
            headers.add(HeaderField.parse("Content-Type", req.representation().contentType()));
        }


        headers.add(HeaderField.parse("X-Forwarded-For", req.connectionInfo().remoteAddress));

        // Support for the de-facto standard "X-Forwarded-Host" header
        //   https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/X-Forwarded-Host
        HeaderField maybeHostHeader = req.header().field("host");
        if(maybeHostHeader!=null){
            headers.add(HeaderField.parse("X-Forwarded-Host", maybeHostHeader.value()));
        }


        final PreparedRequest prep = new PreparedRequest(url, req.representation(), query, headers.toArray(new HeaderField[0]), client.resource(url));

        System.out.println("Prepared request: " + prep);

        return prep;
    }

    private Response handleResponse(Response response) {
        System.out.println("Handling response:" + response);
        try {
            int codeValue = response.code().value();

            ResponseCode responseCode = ResponseCode.forCode(codeValue);
            if (responseCode == null) {
                log.error("Unknown response code: " + codeValue);
            } else {
                log.debug("Response was " + responseCode);
            }

            List<HeaderField> headersReturned = extractResponseHeaders(response);

            return createResponse(response, headersReturned);

        } catch (Exception e) {
            log.error("Error proxying", e);
            return BAD_GATEWAY();
        }
    }


    private List<HeaderField> extractResponseHeaders(Response response) {
        final List<HeaderField> headersReturned = new ArrayList<HeaderField>();
        for (HeaderField h : response.header()) {
            log.debug("Found header: " + h.name() + "=" + h.value());
            final String name = h.name();
            final String value = h.value();
            if (name.toLowerCase().equals("set-cookie")) {
                final SetCookieField setCookieField = SetCookieField.fromHeaderValue(value);
                log.debug("Cookie found: " + setCookieField);
                headersReturned.add(setCookieField);
            } else if (name.toLowerCase().equals("location")) {
                final String a = processRedirect(value);
                log.debug("Redirecting to " + a);
                headersReturned.add(new LocationField(a));
            } else {
                headersReturned.add(HeaderField.parse(name, value));
            }
        }
        return headersReturned;
    }

    private String processRedirect(String url) {
        String a = url.replaceAll(Pattern.quote(base), me);
        return a;
    }

    private HeaderField getHeader(String name, HeaderField[] headers){
        for(HeaderField header : headers){
            if(header.name().toLowerCase().equals(name.toLowerCase())){
                return header;
            }
        }
        return null;
    }

    private Response createResponse(Response response, List<HeaderField> headersReturned) {

        return new Response(response.code(), response.hasRepresentation() ? new Representation() {
            @Override
            public String contentType() {
                HeaderField h = getHeader("Content-Type", response.header());
                return h == null ? null : h.value();
            }

            @Override
            public void write(OutputStream out) {
                response.representation().write(out);
            }
        } : null,
        headersReturned.toArray(new HeaderField[]{}));
    }

    private static String stripTrailingSlash(String text) {
        if (text.endsWith("/") && text.length() > 1) {
            return text.substring(0, text.length() - 1);
        } else {
            return text;
        }
    }

}
