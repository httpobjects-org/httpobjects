package org.httpobjects.websockets

interface WebSocketChannel {
    fun id():String
    fun writeAndFlush(frame:WebSocketFrame):EventualResult<Unit>
    fun close():EventualResult<Unit>
}