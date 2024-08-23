package org.httpobjects.websockets

interface WebSocketClient {
    fun <T:WebSocketChannelHandler>connect(url:String, headers:Map<String, String>, onDisconnect:(T)->Unit, beginSession:(context: WebSocketChannel)->T):T?
}