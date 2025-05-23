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
package org.httpobjects.representation;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.httpobjects.DSL;
import org.httpobjects.StandardCharset;
import org.junit.Test;

public class BinaryRepresentationTest {

    @Test
    public void leavesInputExceptionsAlone() {
        // given
        final RuntimeException connectionException = new RuntimeException("Test exception please ignore");
        final InputStream in = new InputStream() {
            @Override
            public int read() throws IOException {
                throw connectionException;
            }
        };
        InputStreamRepresentation r = new InputStreamRepresentation("foo/bar", in);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        
        // when
        Throwable err = null;
        try {
            r.data().writeSync(out);
        } catch (Exception e) {
            err = e;
        }
        
        // then
        assertNotNull(err);
        assertTrue(err  == connectionException);
        err.printStackTrace();
    }
    
    @Test
    public void wrapsOutputExceptionsWithAHelpfulMessage() {
        // given
        final EOFException connectionException = new EOFException();
        InputStreamRepresentation r = new InputStreamRepresentation("foo/bar", new ByteArrayInputStream(DSL.getBytes("foobar", StandardCharset.UTF_8)));
        OutputStream out = new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                throw connectionException;
            }
        };
        
        // when
        Throwable err = null;
        try {
            r.data().writeSync(out);
        } catch (Exception e) {
            err = e;
        }
        
        // then
        assertNotNull(err);
        assertEquals("Error writing representation.  This is probably because the connection to the remote host was closed.", err.getMessage());
        assertTrue(err.getCause() == connectionException);
        err.printStackTrace();
    }

}
