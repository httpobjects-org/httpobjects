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
package org.httpobjects.header;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class Header {
	private final List<HeaderField> fields;

	public Header(List<HeaderField> fields){
		this.fields = Collections.unmodifiableList(fields);
	}

	public List<HeaderField> fields() {
		return fields;
	}

	public HeaderField field(String name){
		for(HeaderField next : fields){
			if(next.name().equals(name)){
				return next;
			}
		}
		return null;
	}

    public String getOrElse(String name, String defaultValue) {
        final HeaderField field = field(name);
        return field==null?defaultValue:field.value();
    }

    public Optional<String> get(String key) {
        return Optional.ofNullable(field(key)).map(HeaderField::value);
    }

	@Override
    public String toString() {
		String pairs = fields.stream().map(HeaderField::show)
				.sorted().collect(Collectors.joining(","));
		return "{" + pairs + "}";
	}

	@Override
	public boolean equals(Object other){
		return (other instanceof Header) && eq((Header)other);
	}
	public boolean eq(Header that) {
		return this.toString().equals(that.toString());
	}
}
