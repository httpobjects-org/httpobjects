package org.httpobjects.websockets

import org.httpobjects.eventual.Eventual

interface WebSocketChannel {
    fun id():String
    fun writeAndFlush(frame:WebSocketFrame): Eventual<Unit>
    fun close(): Eventual<Unit>
}