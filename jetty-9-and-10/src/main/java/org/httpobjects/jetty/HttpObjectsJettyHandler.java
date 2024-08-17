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

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.httpobjects.HttpObject;
import org.httpobjects.header.GenericHeaderField;
import org.httpobjects.header.HeaderField;
import org.httpobjects.servlet.ServletMethodInvoker;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;


public class HttpObjectsJettyHandler extends org.eclipse.jetty.server.handler.AbstractHandler {
    private final ServletMethodInvoker invoker;

    public HttpObjectsJettyHandler(HttpObject... objects) {
        this(Collections.<HeaderField>emptyList(), objects);
    }


    public HttpObjectsJettyHandler(List<? extends HeaderField> defaultResponseHeaders, HttpObject... objects) {
        invoker = new ServletMethodInvoker(defaultResponseHeaders, HttpObject.NOT_FOUND(HttpObject.Text("Error: NOT_FOUND")), objects);
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) {

        try {
            final boolean invocationHappened = invoker.invokeFirstPathMatchIfAble(target, request, response);

            if (invocationHappened)
                setHandled(baseRequest);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Unhandled error while processing target " + target + ": " + e.getClass().getSimpleName() + " - " + e.getMessage(), e);
        }

    }

    private void setHandled(Request baseRequest) {
        baseRequest.setHandled(true);
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
}
