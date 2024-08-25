package org.httpobjects.kotlin

import org.httpobjects.DateTimeRFC6265
import org.httpobjects.HttpObject
import org.httpobjects.eventual.Eventual
import org.httpobjects.header.response.SetCookieField
import org.httpobjects.util.PathMap
import org.httpobjects.util.PathMapEntry

fun <T> resolved(fn:()->T):Eventual<T> = Eventual.resolveTo(fn)

fun resolveNoShadows(vararg resourcesByPathPattern:Pair<String, HttpObject>) = PathMap.resolveNoShadows(
    resourcesByPathPattern.map{(path, o) -> PathMapEntry(path, o) }
)

fun resolveNoShadows(resourcesByPathPattern:List<Pair<String, HttpObject>>) = PathMap.resolveNoShadows(
    resourcesByPathPattern.map{(path, o) -> PathMapEntry(path, o) }
)

fun validateNoShadows(vararg resourcesByPathPattern:Pair<String, HttpObject>) = PathMap.validateNoShadows(
    resourcesByPathPattern.map{(path, o) -> PathMapEntry(path, o) }
)

fun validateNoShadows(resourcesByPathPattern:List<Pair<String, HttpObject>>) = PathMap.validateNoShadows(
    resourcesByPathPattern.map{(path, o) -> PathMapEntry(path, o) }
)

fun SetCookie(
    name: String,
    value: String?,
    domain: String? = null,
    path: String? = null,
    expiration: DateTimeRFC6265? = null,
    secure: Boolean? = null,
    httpOnly: Boolean? = null,
    sameSite: String? = null) = SetCookieField(name, value, domain, path, expiration?.toString(), secure, httpOnly, sameSite)
