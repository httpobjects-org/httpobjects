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
package org.httpobjects.header.request;

import java.util.StringTokenizer;

import org.httpobjects.header.HeaderField;
import org.httpobjects.header.HeaderFieldVisitor;
import org.httpobjects.header.request.credentials.BasicCredentials;
import org.httpobjects.header.response.WWWAuthenticateField;
import org.httpobjects.header.response.WWWAuthenticateField.Method;
import org.httpobjects.impl.Base64;

public class AuthorizationField extends HeaderField {

	public static AuthorizationField parse(String s) throws ParsingException {
		String fValue = s.trim();
		StringTokenizer tokens = new StringTokenizer(fValue);

		if (tokens.countTokens() < 2)
			throw new ParsingException(ParsingException.Failure.MISSING_SCHEME);

		Method method;
		try {
			method = Method.valueOf(tokens.nextToken());
		} catch (IllegalArgumentException ex) {
			throw new ParsingException(ParsingException.Failure.UNSUPPORTED_SCHEME);
		}

		AuthorizationField f = new AuthorizationField(method, tokens.nextToken());
		return f;
	}
	
	private final WWWAuthenticateField.Method method;
	private final String rawCredentials;
	
	public AuthorizationField(Method method, String rawCredentials) {
		super();
		this.method = method;
		this.rawCredentials = rawCredentials;
	}

	@Override
	public <T> T accept(HeaderFieldVisitor<T> visitor) {
		return visitor.visit(this);
	}
	
	public WWWAuthenticateField.Method method() {
		return method;
	}
	public String rawCredentials() {
		return rawCredentials;
	}
	
	public String credentialsString(){
		return new String(Base64.decode(rawCredentials));
	}
	
	public BasicCredentials basicCredentials(){
		if(this.method == Method.Basic){
			return BasicCredentials.parse(credentialsString());
		}else{
			return null;
		}
	}

	@Override
    public String name() {
        return "authorization";
    }

    @Override
    public String value() {
        return method + " " + rawCredentials;
    }

	@Override
	public String toString() {
		return getClass().getSimpleName() + "(\"" + value() + "\")";
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		AuthorizationField that = (AuthorizationField) o;

		if (method != that.method) return false;
		return rawCredentials != null ? rawCredentials.equals(that.rawCredentials) : that.rawCredentials == null;
	}

	@Override
	public int hashCode() {
		int result = method != null ? method.hashCode() : 0;
		result = 31 * result + (rawCredentials != null ? rawCredentials.hashCode() : 0);
		return result;
	}

	public static class ParsingException extends RuntimeException {
		public enum Failure {
			MISSING_SCHEME("missing authorization scheme"),
			UNSUPPORTED_SCHEME("unsupported authorization scheme");

			private final String message;

			Failure(String message) {
				this.message = message;
			}

			public String getMessage() {
				return message;
			}
		}

		private final Failure failure;

		public ParsingException(Failure failure) {
			super(failure.getMessage());
			this.failure = failure;
		}

		public Failure getFailure() {
			return failure;
		}
	}
}
