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

import org.httpobjects.eventual.BasicEventualResult;
import org.httpobjects.eventual.EventualResult;
import org.httpobjects.path.Path;
import org.httpobjects.path.PathParamName;
import org.httpobjects.path.PathPattern;
import org.httpobjects.path.SimplePathPattern;
import org.httpobjects.util.HttpObjectUtil;
import org.httpobjects.util.Method;

import java.util.ArrayList;
import java.util.List;

public class HttpObject extends DSL{

    private final PathPattern pathPattern;
    private final EventualResult<Response> defaultResponse;

    public HttpObject(PathPattern pathPattern, Response defaultResponse) {
        super();
        this.pathPattern = pathPattern;
        this.defaultResponse = defaultResponse == null ? null : new BasicEventualResult(defaultResponse);
    }

    public HttpObject(String pathPattern, Response defaultResponse) {
        this(new SimplePathPattern(pathPattern), defaultResponse);
    }

    public HttpObject(PathPattern pathPattern) {
        this(pathPattern, METHOD_NOT_ALLOWED());
    }

    public HttpObject(String pathPattern) {
        this(new SimplePathPattern(pathPattern));
    }

    public PathPattern pattern() {
        return pathPattern;
    }

    public EventualResult<Response> delete(Request req){return defaultResponse;}
    public EventualResult<Response> get(Request req){return defaultResponse;}
    public EventualResult<Response> head(Request req){return defaultResponse;}
    public EventualResult<Response> options(Request req){return defaultResponse;}
    public EventualResult<Response> post(Request req){return defaultResponse;}
    public EventualResult<Response> put(Request req){return defaultResponse;}
    public EventualResult<Response> trace(Request req){return defaultResponse;}
    public EventualResult<Response> patch(Request req){return defaultResponse;}

}
