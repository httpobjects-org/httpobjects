package org.httpobjects.websockets

import org.httpobjects.eventual.EventualResult

interface WebSocketChannel {
    fun id():String
    fun writeAndFlush(frame:WebSocketFrame): EventualResult<Unit>
    fun close(): EventualResult<Unit>
}