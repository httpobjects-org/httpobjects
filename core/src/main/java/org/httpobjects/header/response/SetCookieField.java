/**
 * <p>
 * Copyright (C) 2011, 2012 Commission Junction Inc.
 * </p>
 * <p>
 * This file is part of httpobjects.
 * </p>
 * <p>
 * httpobjects is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 * </p>
 * <p>
 * httpobjects is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * </p>
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with httpobjects; see the file COPYING.  If not, write to the
 * Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301 USA.
 * </p>
 * <p>
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole
 * combination.
 * </p>
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
 * </p>
 */
package org.httpobjects.header.response;

import org.httpobjects.DateTimeRFC6265;
import org.httpobjects.header.HeaderField;
import org.httpobjects.header.HeaderFieldVisitor;

import java.util.StringTokenizer;

public class SetCookieField extends HeaderField {
    enum SameSiteValue{Lax, Strict, None}

    public static final SetCookieField fromHeaderValue(String header) {
        StringTokenizer s = new StringTokenizer(header);
        NameValue nameValue = NameValue.parse(s.nextToken(";"));

        String domain = null;
        String path = null;
        String expiration = null;
        Boolean secure = null;
        Boolean httpOnly = null;
        String sameSite = null;

        while (s.hasMoreTokens()) {
            String next = s.nextToken(";");
            if (next != null) {
                next = next.trim();
                if (next.equalsIgnoreCase("Secure")) {
                    secure = true;
                } else if (next.equalsIgnoreCase("HttpOnly")) {
                    httpOnly = true;
                } else {
                    NameValue property = NameValue.parse(next.trim());
                    String name = property.name.toLowerCase();

                    if (name.equals("domain")) {
                        domain = property.value;
                    } else if (name.equals("path")) {
                        path = property.value;
                    } else if (name.equals("expires")) {
                        expiration = property.value;
                    } else if (name.equals("samesite")) {
                        sameSite = property.value;
                    }
                }
            }
        }
        return new SetCookieField(nameValue.name, nameValue.value, domain, path, expiration, secure, httpOnly, sameSite);
    }

    public boolean isSecure() {
        return secure == null ? false : secure;
    }

    private static class NameValue {
        public static NameValue parse(String attributeString) {
            try {
                final String name, value;

                final int pos = attributeString.indexOf('=');
                if (pos == -1) {
                    name = attributeString;
                    value = null;
                } else {
                    name = attributeString.substring(0, pos);
                    value = stripQuotes(attributeString.substring(pos + 1));
                }

                return new NameValue(name, value);
            } catch (Exception e) {
                throw new RuntimeException("Error parsing " + attributeString);
            }
        }

        private static String stripQuotes(String value) {
            if (value.startsWith("\"") && value.endsWith("\"")) {
                value = value.substring(1, value.length() - 1);
            }
            return value;
        }

        private final String name, value;

        public NameValue(String name, String value) {
            super();
            this.name = name;
            this.value = value;
        }
    }

    public final String name;
    public final String value;
    public final String domain;
    public final String path;
    public final String expiration;
    /*
     * TODO: parse cookie dates instead of just passing that responsibility on to the user?
     *   https://tools.ietf.org/html/rfc6265#section-5.1.1
     *
     * TODO: support Max-Age??
     *   https://mrcoles.com/blog/cookies-max-age-vs-expires/
     */
    public final Boolean secure;
    public final Boolean httpOnly;
    public final String sameSite ;

    public SetCookieField(String name, String value, String domain, String path,
                          String expiration, Boolean secure, Boolean httpOnly,
                          String sameSite) {
        this.name = name;
        this.value = value;
        this.domain = domain;
        this.path = path;
        this.expiration = expiration;
        this.secure = secure;
        this.httpOnly = httpOnly;
        this.sameSite = sameSite;
    }

    public SetCookieField(String name, String value, String domain, String path,
                          DateTimeRFC6265 expiration, Boolean secure, Boolean httpOnly,
                          String sameSite) {
        this(name, value, domain,
             path, expiration.toString(), secure, httpOnly, sameSite);
    }

    public SetCookieField(String name, String value, String domain, String path,
                          DateTimeRFC6265 expiration, Boolean secure, Boolean httpOnly) {
        this(name, value, domain,
             path, expiration, secure, httpOnly, null);
    }

    public SetCookieField(String name, String value, String domain, String path,
                          String expiration, Boolean secure, Boolean httpOnly) {
        this(name, value, domain,
             path, expiration, secure, httpOnly, null);
    }

    public SetCookieField(String name, String value, String domain, String path,
                          String expiration, Boolean secure) {
        this(name, value, domain, path, expiration, secure, null);
    }

    public SetCookieField(String name, String value, String domain) {
        this(name, value, domain, null, null, null);
    }


    public DateTimeRFC6265 parsedExpiration(){
        return new DateTimeRFC6265(expiration);
    }
    public SameSiteValue parseSameSite(){
        SameSiteValue match = null;

        String sameSiteLowercase = this.sameSite.toLowerCase();
        for(SameSiteValue n : SameSiteValue.values()){
            if(n.name().toLowerCase().equals(sameSiteLowercase)){{
                match = n;
            }}
        }

        return match;
    }

    @Override
    public <T> T accept(HeaderFieldVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public String name() {
        return "Set-Cookie";
    }

    @Override
    public String value() {
        String base = name + "=" + value;

        String secure = (this.secure != null && this.secure) ? " Secure;" : "";
        String httpOnly = (this.httpOnly != null && this.httpOnly) ? " HttpOnly;" : "";

        String tail = appendFieldIfNotNull("Domain", domain) +
                appendFieldIfNotNull("Path", path) +
                appendFieldIfNotNull("Expires", expiration) +
                secure +
                httpOnly;

        return tail.isEmpty() ? base : base + ";" + tail;
    }


    @Override
    public String toString() {
        String base = name + "=" + value;

        String secure = (this.secure != null && this.secure) ? " Secure;" : "";
        String httpOnly = (this.httpOnly != null && this.httpOnly) ? " HttpOnly;" : "";

        String tail = appendFieldIfNotNull("Domain", domain) +
                appendFieldIfNotNull("Path", path) +
                appendFieldIfNotNull("Expires", expiration) +
                secure +
                httpOnly +
                appendFieldIfNotNull("SameSite", sameSite);

        return tail.isEmpty() ? base : base + ";" + tail;
    }

    private String appendFieldIfNotNull(String name, String value) {
        return value == null ? "" : " " + name + "=" + value + ";";
    }

    public boolean isHttpOnly() {
        return httpOnly == null ? false : httpOnly;
    }
}
