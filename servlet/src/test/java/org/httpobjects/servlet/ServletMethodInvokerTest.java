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
package org.httpobjects.servlet;

import org.httpobjects.HttpObject;
import org.httpobjects.Response;
import org.httpobjects.header.HeaderField;
import org.junit.Ignore;
import org.junit.Test;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.*;
import java.security.Principal;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ServletMethodInvokerTest {

    @Test
    public void gracefullyHandlesUnknownHttpMethods() {
        //given
        HttpObject foo = new HttpObject("/foo/baz");
        HttpObject[] objects = new HttpObject[]{
                foo,
        };

        FakePathMatchObserver pathMatchObserver = new FakePathMatchObserver();
        ServletMethodInvoker servletMethodInvoker = new ServletMethodInvoker(
                pathMatchObserver,
                Collections.<HeaderField>emptyList(),
                HttpObject.NOT_FOUND(HttpObject.Text("Error: NOT_FOUND")),
                objects
        );

        HttpServletRequest request = new TestHttpServletRequest("crazymethod", new HashMap<>());
        TestHttpServletResponse response = new TestHttpServletResponse();

        //when
        boolean invokedAndGotNonNullResponse = servletMethodInvoker.invokeFirstPathMatchIfAble("/foo/baz", request, response);

        //then
        assertTrue(invokedAndGotNonNullResponse);
        assertEquals(1, pathMatchObserver.checkingPathAgainstPatternInvocations.size());
        assertEquals(1, pathMatchObserver.pathMatchedPatternInvocations.size());
    }
    
    @Ignore("gotta figure out a simpler way to test this, it is currently fumbling on the request response, but the real problem is we need to decouple concerns into testable chunks")
    public void weCanDebugWhichPathPatternMatched() {
        //given
        HttpObject foo = new HttpObject("/foo/baz");
        HttpObject expected = new HttpObject("/user/{id}/account/{name}");
        HttpObject bar = new HttpObject("/bar/baz");
        HttpObject[] objects = new HttpObject[]{
                foo,
                expected,
                bar,
        };
        Response notFoundResponse = HttpObject.NOT_FOUND(HttpObject.Text("Error: NOT_FOUND"));
        List<? extends HeaderField> defaultResponseHeader = Collections.<HeaderField>emptyList();
        FakePathMatchObserver pathMatchObserver = new FakePathMatchObserver();
        ServletMethodInvoker servletMethodInvoker = new ServletMethodInvoker(
                pathMatchObserver, defaultResponseHeader, notFoundResponse, objects);

        //when
        boolean invokedAndGotNonNullResponse = servletMethodInvoker.invokeFirstPathMatchIfAble("/user/123/account/blah", null, null);

        //then
        assertTrue(invokedAndGotNonNullResponse);
        assertEquals(2, pathMatchObserver.checkingPathAgainstPatternInvocations.size());
        assertEquals("/user/123/account/blah", pathMatchObserver.checkingPathAgainstPatternInvocations.get(0).path);
        assertEquals("/foo/baz", pathMatchObserver.checkingPathAgainstPatternInvocations.get(0).pathPattern.raw());
        assertEquals("/user/123/account/blah", pathMatchObserver.checkingPathAgainstPatternInvocations.get(1).path);
        assertEquals("/user/{id}/account/{name}", pathMatchObserver.checkingPathAgainstPatternInvocations.get(1).pathPattern.raw());
        assertEquals(1, pathMatchObserver.pathMatchedPatternInvocations.size());
        assertEquals("/user/123/account/blah", pathMatchObserver.pathMatchedPatternInvocations.get(0).path);
        assertEquals("/user/{id}/account/{name}", pathMatchObserver.pathMatchedPatternInvocations.get(0).pathPattern.raw());
    }

}

