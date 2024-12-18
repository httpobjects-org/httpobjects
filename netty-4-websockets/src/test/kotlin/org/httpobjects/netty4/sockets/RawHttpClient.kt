package org.httpobjects.netty4.sockets

import java.io.InputStream


data class ParsedResponse(val statusLine:String, val headers:List<Pair<String, String>>){
    fun headerValue(n:String):List<String> = headers.filter { it.first.equals(n, ignoreCase = true) }.map { it.second }
}
fun parseResponse(input: InputStream):ParsedResponse{
    println("Reading")

    val responseHeader = input.lines().takeWhile { it.isNotBlank() }.toList()

    responseHeader.forEach(::println)

    return ParsedResponse(
        statusLine = responseHeader.first(),
        headers = responseHeader.drop(1).map {
            it.substring(0, it.indexOf(':')) to it.substring(it.indexOf(':')+1).trimStart()
        }
    )
}