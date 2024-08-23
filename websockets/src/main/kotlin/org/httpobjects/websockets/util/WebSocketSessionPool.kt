package org.httpobjects.websockets.util

import org.httpobjects.websockets.WebSocketChannelHandler
import org.httpobjects.websockets.WebSocketChannel
import org.httpobjects.websockets.WebSocketClient

/**
 * For when you want to maintain and re-use active websockets connections to a set of URLs
 * TODO: handle disconnects & keep-alives
 */
class WebSocketSessionPool<T: WebSocketChannelHandler>(
    private val client: WebSocketClient,
    private val beginSession:(context: WebSocketChannel)->T) {
    private val mutex = Object();
    private val pooledClients:MutableMap<String, T> = mutableMapOf()

    fun sessions():Map<String, T> = pooledClients.toMap()

    fun openOrReuseSession(url:String): T = synchronized(mutex){
        val existing = pooledClients[url]

        val entry = if(existing==null){
            val newClient = client.connect(
                url = url,
                headers = mapOf(),
                onDisconnect = {
                    pooledClients.remove(url)
                },
                beginSession = { context ->
                    val session = beginSession(context)
                   log("pooling connection/session for url=$url, session=$session")

                    session
                })!!

            pooledClients[url] = newClient

            newClient
        }else{
            existing
        }

        return entry
    }

    private fun log(m:String) = println("[${this.javaClass.simpleName}] $m")
}