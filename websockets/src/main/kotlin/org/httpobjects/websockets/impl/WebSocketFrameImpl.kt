package org.httpobjects.websockets.impl

import org.httpobjects.websockets.*
import java.nio.charset.Charset


class BasicGarbageCollectedBinaryWebSocketFrame(private val data:FrameData):BinaryWebSocketFrame{
    override fun data() = data
    override fun release() {}
    override fun retain() {}
}
class BasicGarbageCollectedContinuationWebSocketFrame(private val data:FrameData):ContinuationWebSocketFrame{
    override fun release() {}
    override fun retain() {}
}

class BasicGarbageCollectedPingWebSocketFrame(private val data:FrameData):PingWebSocketFrame{
    override fun data() = data
    override fun release() {}
    override fun retain() {}
}
class BasicGarbageCollectedPongWebSocketFrame(private val data:FrameData):PongWebSocketFrame{
    override fun data() = data
    override fun release() {}
    override fun retain() {}
}

class BasicGarbageCollectedTextWebSocketFrame(private val array:ByteArray):TextWebSocketFrame{
    private val text:String by lazy {
        array.toString(textEncoding)
    }
    constructor(text:String):this(text.toByteArray(textEncoding))

    override fun data() = ArrayFrameData(this.array)
    override fun release() {}
    override fun retain() {}
    override fun text() = this.text

    companion object {
        val textEncoding = Charset.forName("UTF-8")
    }
}

