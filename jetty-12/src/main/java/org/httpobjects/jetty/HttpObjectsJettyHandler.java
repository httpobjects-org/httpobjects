/**
 * Copyright (C) 2011, 2012 Commission Junction Inc.
 * <p>
 * This file is part of httpobjects.
 * <p>
 * httpobjects is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 * <p>
 * httpobjects is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with httpobjects; see the file COPYING.  If not, write to the
 * Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301 USA.
 * <p>
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole
 * combination.
 * <p>
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
package org.httpobjects.jetty;

import org.eclipse.jetty.io.Content;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.util.Callback;
import org.httpobjects.HttpObject;
import org.httpobjects.eventual.Eventual;
import org.httpobjects.header.GenericHeaderField;
import org.httpobjects.header.HeaderField;
import org.httpobjects.header.HeaderFieldVisitor;
import org.httpobjects.header.request.AuthorizationField;
import org.httpobjects.header.request.CookieField;
import org.httpobjects.header.response.AllowField;
import org.httpobjects.header.response.LocationField;
import org.httpobjects.header.response.SetCookieField;
import org.httpobjects.header.response.WWWAuthenticateField;
import org.httpobjects.util.HttpObjectUtil;
import org.httpobjects.util.Method;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;


public class HttpObjectsJettyHandler extends org.eclipse.jetty.server.handler.AbstractHandler {
    private final HttpObject[] objects;
    private final org.httpobjects.Response notFoundResponse;
    private final List<? extends HeaderField> defaultResponseHeaders;

    public HttpObjectsJettyHandler(HttpObject... objects) {
        this(Collections.<HeaderField>emptyList(), objects);
    }


    public HttpObjectsJettyHandler(List<? extends HeaderField> defaultResponseHeaders, HttpObject... objects) {
        this.defaultResponseHeaders = defaultResponseHeaders;
        this.objects = objects;
        this.notFoundResponse = HttpObject.NOT_FOUND(HttpObject.Text("Error: NOT_FOUND"));
    }

    public boolean invokeFirstPathMatchIfAble(Request r, Response httpResponse) {
        final String path = r.getHttpURI().getPath();

        org.httpobjects.Response lastResponse = null;
        for (HttpObject next : objects) {
            if (next.pattern().matches(path)) {
                lastResponse = invoke(path, r, next);
                if (lastResponse != null) {
                    returnResponse(lastResponse, httpResponse);
                    break;
                }
            }
        }

        if (lastResponse != null) {
            return true;
        } else if (notFoundResponse != null) {
            returnResponse(notFoundResponse, httpResponse);
            return true;
        } else {
            return false;
        }
    }


    private org.httpobjects.Response invoke(String path, Request r, HttpObject object) {
        final Method m = Method.fromString(r.getMethod());
        final org.httpobjects.Request input = new ImmutableRequestImpl(object.pattern().match(path), r);

        if(m==null){
            System.out.println("WARNING: not a method I know about: " + r.getMethod());
        }

        final Eventual<org.httpobjects.Response> eventual = HttpObjectUtil.invokeMethod(object, m, input);
        return eventual ==null ? null : eventual.join();
    }

    private void returnResponse(org.httpobjects.Response r, final Response resp)  {

        try {
            resp.setStatus(r.code().value());

            for(HeaderField next : r.header()){
                next.accept(new HeaderFieldVisitor() {

                    @Override
                    public Void visit(CookieField cookieField) {
                        resp.getHeaders().add(cookieField.name(), cookieField.value());
                        return null;
                    }

                    @Override
                    public Void visit(GenericHeaderField other) {
                        // TODO: This might not work right with multiple headers of the same name
                        resp.getHeaders().add(other.name(), other.value());
                        return null;
                    }

                    @Override
                    public Void visit(AllowField allowField) {
                        resp.getHeaders().add(allowField.name(), allowField.value());
                        return null;
                    }

                    @Override
                    public Void visit(LocationField location) {
                        resp.getHeaders().add(location.name(), location.value());
                        return null;
                    }

                    @Override
                    public Void visit(SetCookieField setCookieField) {
                        resp.getHeaders().add(setCookieField.name(), setCookieField.value());
                        return null;
                    }

                    @Override
                    public Void visit(WWWAuthenticateField wwwAuthorizationField) {
                        resp.getHeaders().add(wwwAuthorizationField.name(), wwwAuthorizationField.value());
                        return null;
                    }
                    @Override
                    public Void visit(AuthorizationField authorizationField) {
                        throw new RuntimeException("Illegal header for request: " + authorizationField.getClass());
                    }

                });
            }

            addDefaultHeadersAsApplicable(r, resp);

            if(r.hasRepresentation()){
                System.out.println("Content type is " + r.representation().contentType());
                final String contentType = r.representation().contentType();
                if(contentType!=null){
                    resp.getHeaders().add("Content-Type", contentType);
                }

                final OutputStream out = Content.Sink.asOutputStream(resp);
                r.representation().write(out);
                out.close();
            }else{
                resp.write(
                    true,
                    ByteBuffer.wrap(new byte[]{}),
                    new Callback(){}
                );
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    private void addDefaultHeadersAsApplicable(final org.httpobjects.Response r, final Response resp) {
        for(HeaderField defaultHeader : defaultResponseHeaders){
            boolean exists = false;
            for(HeaderField header : r.header()){
                if(header.name().equals(defaultHeader.name())){
                    exists = true;
                }
            }

            if(!exists){
                resp.getHeaders().add(defaultHeader.name(), defaultHeader.value());
            }
        }
    }

    @Override
    public boolean handle(Request request, Response response, Callback callback) throws Exception {
        try {
            return invokeFirstPathMatchIfAble(request, response);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Unhandled error while processing target " + request.getHttpURI() + ": " + e.getClass().getSimpleName() + " - " + e.getMessage(), e);
        }
    }


    public static Server launchServer(int port, HttpObject... objects) {
        try {
            Server s = new Server(port);
            s.setHandler(new HttpObjectsJettyHandler(Collections.singletonList(new GenericHeaderField("Cache-Control", "no-cache")), objects));

            s.start();

            return s;
        } catch (Exception e) {
            throw new RuntimeException("" + e.getMessage() + " (port = " + port + ")", e);
        }
    }

    public static Server launchServer(int port, int idleTimeout, HttpObject... objects) {
        try {
            Server s = new Server();

            ServerConnector connector = new ServerConnector(s);
            connector.setPort(port);
            connector.setIdleTimeout(idleTimeout);
            s.setConnectors(new Connector[]{connector});

            s.setHandler(new HttpObjectsJettyHandler(Collections.singletonList(new GenericHeaderField("Cache-Control", "no-cache")), objects));

            s.start();

            return s;
        } catch (Exception e) {
            throw new RuntimeException("" + e.getMessage() + " (port = " + port + ")", e);

        }
    }

    //Replacement for launchServer.
    //Easier to test because the jetty server is hidden behind an interface.
    //Replaced checked exceptions with unchecked exceptions.
    public static JettyServerContract launchServerContract(int port, HttpObject... objects){
        JettyServerContract server = new JettyServerDelegate(new Server(port));
        server.setHandler(new HttpObjectsJettyHandler(Collections.singletonList(new GenericHeaderField("Cache-Control", "no-cache")), objects));
        server.start();
        return server;
    }

    @Override
    public <T> java.util.Collection<T> getCachedBeans(Class<T> clazz) {
        return super.getCachedBeans(clazz);
    }

    @Override
    public boolean isDumpable(Object o) {
        return super.isDumpable(o);
    }

    @Override
    public String dumpSelf() {
        return super.dumpSelf();
    }
}
