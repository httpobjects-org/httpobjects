package org.httpobjects.websockets

sealed interface WebSocketChannelEvent {}
object ChannelDisconnected: WebSocketChannelEvent
class FrameReceived(val frame: WebSocketFrame): WebSocketChannelEvent

abstract class WebSocketChannelHandler(val channel: WebSocketChannel) {
    abstract fun handleEvent(event: WebSocketChannelEvent)
}