package org.httpobjects.websockets

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame
import io.netty.handler.codec.http.websocketx.ContinuationWebSocketFrame
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame
import io.netty.handler.codec.http.websocketx.WebSocketFrame
import org.httpobjects.eventual.Resolvable
import org.httpobjects.eventual.Eventual
import org.httpobjects.websockets.impl.*
import java.io.OutputStream

fun FrameData.toByteBuf(): ByteBuf = Unpooled.wrappedBuffer(this.arrayCopy())

fun toNettyFrame(frame: org.httpobjects.websockets.WebSocketFrame): WebSocketFrame {
    fun data(frame:WebSocketDataFrame) = frame.data().toByteBuf()

    return when(frame){
        is org.httpobjects.websockets.BinaryWebSocketFrame -> BinaryWebSocketFrame(data(frame))
        is org.httpobjects.websockets.CloseWebSocketFrame -> CloseWebSocketFrame(frame.statusCode(), frame.reasonText())
        is org.httpobjects.websockets.ContinuationWebSocketFrame -> TODO("ContinuationWebSocketFrame NOT IMPLEMENTED")
        is org.httpobjects.websockets.PingWebSocketFrame -> PingWebSocketFrame(data(frame))
        is org.httpobjects.websockets.PongWebSocketFrame -> PongWebSocketFrame(data(frame))
        is org.httpobjects.websockets.TextWebSocketFrame ->  TextWebSocketFrame(frame.text())
        else -> throw Exception("Don't know how to translate $frame")
    }
}
fun toHttpObjectsFrame(frame: WebSocketFrame): org.httpobjects.websockets.WebSocketFrame {
    val c = frame.content()
    val data: FrameData = object: FrameData {
        override fun write(out: OutputStream) {
            c.readBytes(out, c.readableBytes())
        }

        override fun write(out: ByteArray) {
            c.readBytes(out)
        }

        override fun readableBytes(): Int = c.readableBytes()
        override fun arrayCopy(): ByteArray {
            val data = ByteArray(readableBytes())
            this.write(out = data)
            return data
        }
    }
    return when(frame){
        is BinaryWebSocketFrame -> BasicGarbageCollectedBinaryWebSocketFrame(data)
        is CloseWebSocketFrame -> object:org.httpobjects.websockets.CloseWebSocketFrame{
            override fun statusCode() = frame.statusCode()
            override fun reasonText() = frame.reasonText()
            override fun release() {
                frame.release()
            }

            override fun retain() {
                frame.retain()
            }
        }
        is ContinuationWebSocketFrame -> BasicGarbageCollectedContinuationWebSocketFrame(data)
        is PingWebSocketFrame -> BasicGarbageCollectedPingWebSocketFrame(data)
        is PongWebSocketFrame ->  BasicGarbageCollectedPongWebSocketFrame(data)
        is TextWebSocketFrame -> BasicGarbageCollectedTextWebSocketFrame(frame.text())
        else -> {
            throw Exception("Unknown frame type: ${frame.javaClass.simpleName} ($frame)")
        }
    }
}


fun wrapChannelFuture(future: ChannelFuture): Eventual<Unit> {
    val result = Resolvable<Unit>()
    future.addListener {
        result.resolve(Unit)
    }
    return result
}

fun toHttpObjectsChannel(context: ChannelHandlerContext):WebSocketChannel  = NettyWebSocketChannel(context)

class NettyWebSocketChannel(private val context: ChannelHandlerContext):WebSocketChannel{
    override fun close() = context.channel().close().let(::wrapChannelFuture)

    override fun id() = context.channel().id().toString()

    override fun writeAndFlush(frame: org.httpobjects.websockets.WebSocketFrame): Eventual<Unit> {
        return wrapChannelFuture(context.channel().writeAndFlush(toNettyFrame(frame)))
    }
}