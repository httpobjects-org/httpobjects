package org.httpobjects.websockets

import org.httpobjects.Request
import org.httpobjects.Response
import org.httpobjects.path.PathPattern

abstract class WebSocketObject(val pathPattern: PathPattern) {
    abstract fun beginSession(request:Request, channel: WebSocketChannel): WebSocketInitiationResponse
}

data class WebSocketInitiationResponse(
    val response:Response?,
    val session: WebSocketChannelHandler?){
    companion object {
        fun startSession(session: WebSocketChannelHandler)  = WebSocketInitiationResponse(response = null, session = session)
        fun denySession(response:Response)  = WebSocketInitiationResponse(response = response, session = null)
    }
}
