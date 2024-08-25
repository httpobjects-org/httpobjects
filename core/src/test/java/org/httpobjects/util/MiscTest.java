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
package org.httpobjects.util;

import static org.hamcrest.CoreMatchers.is;
import static org.httpobjects.DSL.OK;
import static org.httpobjects.DSL.Text;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.httpobjects.*;
import org.httpobjects.eventual.Eventual;
import org.httpobjects.test.MockRequest;
import org.junit.Test;

public class MiscTest {

    static class PatchTestingObject extends HttpObject {
        final Response response;
        final List<Request> requestsReceived = new ArrayList<Request>();
        
        public PatchTestingObject(String pathPattern, Response response) {
            super(pathPattern);
            this.response = response;
        }

        @Override
        public Eventual<Response> patch(Request req) {
            requestsReceived.add(req);
            return response.resolved();
        }
    }

    @Test
    public void pipesInputsAndOutputsToThePatchMethod() {
        // given
        final Response expectedResponse = OK(Text("Hello WOrld"));
        final PatchTestingObject o = new PatchTestingObject("/foo", expectedResponse);

        final MockRequest input = new MockRequest(o, Method.PATCH, "/foo");

        // when
        Response result = input.invoke();

        // then
        assertNotNull(result);
        assertTrue(expectedResponse == result);
        assertEquals(1, o.requestsReceived.size());
        assertTrue(input == o.requestsReceived.get(0));

    }

    @Test
    public void representationToAscii() {
        // given
        Representation body = Text("Hello World!");
        String actual = body.data().decodeToAscii(Integer.MAX_VALUE);
        assertThat(actual, is("Hello World!"));
    }

    @Test
    public void representationToUtf8() {
        // given
        Representation body = Text("Hello World!");
        String actual = body.data().decodeToUTF8(Integer.MAX_VALUE);;
        assertThat(actual, is("Hello World!"));
    }

    @Test
    public void representationToString() {
        // given
        Representation body = Text("Hello World!");
        String actual = body.data().decodeToUTF8(Integer.MAX_VALUE);
        assertThat(actual, is("Hello World!"));
    }
}
