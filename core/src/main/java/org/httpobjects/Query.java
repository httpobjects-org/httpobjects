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

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.httpobjects.path.Path;
import org.httpobjects.util.RequestQueryUtil;

public final class Query {
    private final String string;

    public Query(String string) {
        super();
        final boolean needsQuestionMark = string!=null && !string.startsWith("?");
        this.string = needsQuestionMark ? "?" + string : string;
    }

    @Override
    public String toString() {
        return string == null ? "" : string;
    }

    public String valueFor(String name) {
        return parse().get(name);
    }

    private Map<String, String> memoizedParams = null;
    private Map<String, String>  parse() {
        try {
            if(memoizedParams==null){
                memoizedParams = RequestQueryUtil.getUrlParameters(string);
            }
            return memoizedParams;
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public List<String> paramNames() {
//        return Collections.emptyList();
        return new ArrayList<String>(parse().keySet());
    }

    @Override
    public boolean equals(Object other){
        return (other instanceof Query) && eq((Query)other);
    }

    public boolean eq(Query that) {
        return this.toString().equals(that.toString());
    }
}
