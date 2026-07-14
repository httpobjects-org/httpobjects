package org.httpobjects.websockets.impl

import org.httpobjects.websockets.*
import java.io.OutputStream
import java.nio.charset.Charset

class BasicGarbageCollectedBinaryWebSocketFrame(private val data: ByteArray):BinaryWebSocketFrame{
    override fun data() = ArrayFrameData(data)
}
class BasicGarbageCollectedContinuationWebSocketFrame(private val data:ByteArray):ContinuationWebSocketFrame

class BasicGarbageCollectedPingWebSocketFrame(private val data:ByteArray):PingWebSocketFrame{
    override fun data() = ArrayFrameData(data)
}
class BasicGarbageCollectedPongWebSocketFrame(private val data:ByteArray):PongWebSocketFrame{
    override fun data() = ArrayFrameData(data)
}

class BasicGarbageCollectedTextWebSocketFrame(private val array:ByteArray):TextWebSocketFrame{
    private val text:String by lazy {
        array.toString(textEncoding)
    }
    constructor(text:String):this(text.toByteArray(textEncoding))

    override fun data() = ArrayFrameData(this.array)
    override fun text() = this.text

    companion object {
        val textEncoding = Charset.forName("UTF-8")
    }
}

