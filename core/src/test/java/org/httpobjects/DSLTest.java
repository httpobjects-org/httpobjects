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
package org.httpobjects;

import static org.junit.Assert.*;

import org.httpobjects.test.MockRequest;
import org.httpobjects.util.ClasspathResourcesObject;
import org.httpobjects.util.HttpObjectUtil;
import org.httpobjects.util.Method;
import org.junit.Test;

public class DSLTest {

    @Test
    public void builderHappyPath(){
    	// given/when
    	ClasspathResourcesObject object = DSL
    			.classpathResourcesAt("/org/httpobjects/util/ClasspathResourcesObjectTest_resources")
    			.servedAt("/");

    	// then
    	assertEquals("/{resource*}", object.pattern().raw());
    	Response response = new MockRequest(object, Method.GET, "/a.txt").invoke();
    	assertEquals("hello", response.representation().data().decodeToAscii(Integer.MAX_VALUE));
    }

    @Test
    public void builderWithDifferentServiceRoot(){
    	// given/when
    	ClasspathResourcesObject object = DSL
    			.classpathResourcesAt("/org/httpobjects/util/ClasspathResourcesObjectTest_resources")
    			.servedAt("/bar");

    	// then
    	assertEquals("/bar/{resource*}", object.pattern().raw());
    	Response response = new MockRequest(object, Method.GET, "/bar/a.txt").invoke();
    	assertEquals("hello", HttpObjectUtil.toAscii(response.representation()));
    }

    @Test
    public void builderHappyPath_withDifferentClasspathPattern(){
    	// given/when
    	ClasspathResourcesObject object = DSL
    			.classpathResourcesAt("/org/httpobjects")
    			.servedAt("/bar");

    	// then
    	assertEquals("/bar/{resource*}", object.pattern().raw());
    	Response response = new MockRequest(object, Method.GET, "/bar/util/ClasspathResourcesObjectTest_resources/a.txt").invoke();
    	assertEquals("hello", HttpObjectUtil.toAscii(response.representation()));
    }

    @Test
	public void allowedShouldBeAnAliasForNOT_ALLOWED() {
    	// given
		Response test = DSL.allowed(Method.GET, Method.POST);
		Response ctrl = DSL.METHOD_NOT_ALLOWED(Method.GET, Method.POST);

		// then
		assertEquals(ctrl.code(), test.code());
		assertEquals(ctrl.hasRepresentation(), test.hasRepresentation());
		assertEquals(ctrl.header().length, test.header().length);
		assertEquals(ctrl.representation().contentType(), test.representation().contentType());
	}
}
