package org.httpobjects.websockets

import org.httpobjects.websockets.impl.BasicGarbageCollectedBinaryWebSocketFrame
import org.httpobjects.websockets.impl.BasicGarbageCollectedPingWebSocketFrame
import org.httpobjects.websockets.impl.BasicGarbageCollectedPongWebSocketFrame
import org.httpobjects.websockets.impl.BasicGarbageCollectedTextWebSocketFrame
import java.io.OutputStream
import java.nio.charset.Charset

sealed interface WebSocketFrame {
    fun release()
    fun retain()
}

sealed interface WebSocketDataFrame:WebSocketFrame{
    fun data():FrameData
}
interface FrameData {
    fun write(out:OutputStream)
    fun write(out:ByteArray)
    fun readableBytes():Int
    fun arrayCopy(): ByteArray
    fun toString(charset: Charset):String = arrayCopy().toString(charset)
}

class ArrayFrameData(private val array: ByteArray):FrameData {
    override fun write(out: OutputStream) {
        out.write(array)
    }

    override fun write(out: ByteArray) {
        this.array.copyInto(out)
    }

    override fun readableBytes(): Int = array.size
    override fun arrayCopy(): ByteArray = byteArrayOf( * this.array)
}

interface CloseWebSocketFrame:WebSocketFrame {
    fun statusCode():Int
    fun reasonText():String
     companion object {
        fun of(statusCode:Int = normalCloseStatus, reasonText:String = ""):CloseWebSocketFrame = BasicCloseWebSocketFrame(statusCode = statusCode, reasonText = reasonText)
    }
}

const val normalCloseStatus = 1000
data class BasicCloseWebSocketFrame(private val statusCode:Int, private val reasonText:String):CloseWebSocketFrame{
    override fun statusCode() = this.statusCode
    override fun reasonText() = this.reasonText
    override fun release() {}
    override fun retain() {}
}

interface BinaryWebSocketFrame:WebSocketDataFrame {
    companion object {
        fun of(array: ByteArray):BinaryWebSocketFrame = BasicGarbageCollectedBinaryWebSocketFrame(ArrayFrameData(array))
    }
}

interface ContinuationWebSocketFrame:WebSocketFrame

interface PingWebSocketFrame:WebSocketDataFrame{
    companion object {
        fun of(array: ByteArray):PingWebSocketFrame {
            return BasicGarbageCollectedPingWebSocketFrame(ArrayFrameData(array))
        }
    }
}


interface PongWebSocketFrame:WebSocketDataFrame{
    companion object {
        fun of(array: ByteArray):PongWebSocketFrame {
            return BasicGarbageCollectedPongWebSocketFrame(ArrayFrameData(array))
        }
    }
}

interface TextWebSocketFrame:WebSocketDataFrame {
    fun text():String

    companion object {
        fun of(text:String):TextWebSocketFrame = BasicGarbageCollectedTextWebSocketFrame(text)
    }
}


fun WebSocketFrame.copy():WebSocketFrame {
    val frame = this
    return when(this){
        is BinaryWebSocketFrame -> BinaryWebSocketFrame.of(this.data().arrayCopy())
//                is CloseWebSocketFrame -> object:CloseWebSocketFrame{
//                    override fun statusCode() = this.statusCode()
//                    override fun reasonText() = frame.reasonText()
//                    override fun release() {
//                        frame.release()
//                    }
//
//                    override fun retain() {
//                        frame.retain()
//                    }
//                }
//                is ContinuationWebSocketFrame -> BasicGarbageCollectedContinuationWebSocketFrame(data)
        is PingWebSocketFrame -> PingWebSocketFrame.of(this.data().arrayCopy())
        is PongWebSocketFrame ->  PongWebSocketFrame.of(this.data().arrayCopy())
        is TextWebSocketFrame -> TextWebSocketFrame.of(this.text())
        else -> {
            throw Exception("Unsupported frame type: ${frame.javaClass.simpleName} ($frame)")
        }
    }
}
fun WebSocketChannelEvent.copy():WebSocketChannelEvent {
    return when(this){
        is ChannelDisconnected -> ChannelDisconnected
        is FrameReceived -> FrameReceived(this.frame.copy())
    }
}