package org.httpobjects.websockets

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.codec.http.websocketx.WebSocketFrame
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory
import org.httpobjects.DSL
import org.httpobjects.impl.HTLog
import org.httpobjects.netty4.HttpObjectsResponder
import org.httpobjects.netty4.HttpobjectsChannelHandler
import org.httpobjects.netty4.ResponseCreationStrategy
import org.httpobjects.netty4.Translate
import org.httpobjects.netty4.buffer.ByteAccumulator
import org.httpobjects.netty4.buffer.ByteAccumulatorFactory
import java.io.ByteArrayInputStream

class HttpObjectsPlusWebsocketsHandler(
    private val sessionsHandlers:List<WebSocketObject>,
    responder:HttpObjectsResponder,
    private val responseCreator:ResponseCreationStrategy,
    buffers:ByteAccumulatorFactory,
    log:org.httpobjects.netty4.Log) : HttpobjectsChannelHandler(responseCreator, responder, buffers, log) {

    private var handshaker: WebSocketServerHandshaker? = null

    private val logs = HTLog(this)

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {

        try {
            if(msg is HttpRequest && isWebsocketsUpgradeRequest(msg)){
                val path = msg.uri()
                val socketObject = sessionsHandlers.firstOrNull { it.pathPattern.matches(path) }
                if(socketObject==null){
                    logs.log("Websockets requested, but no websockets handler for path: $path.  Handling as regular HTTP")
                    super.channelRead(ctx, msg)
                }else{
                    logs.log("Upgrading to websockets : $path")

                    val connectionInfo = Translate.connectionInfo(ctx)

                    val httpObjectsRequest = Translate.readRequest(
                        socketObject.pathPattern,
                        msg,
                        EmptyReadOnlyAccumulator,
                        connectionInfo)

                    val initiationResult = try{
                        socketObject.beginSession(httpObjectsRequest, toHttpObjectsChannel(ctx))
                    }catch (t: Throwable){
                        t.printStackTrace()
                        WebSocketInitiationResponse.denySession(DSL.INTERNAL_SERVER_ERROR(DSL.Text("An error was encountered while deciding whether to upgrade the connection to a websocket")))
                    }

                    val session = initiationResult.session
                    if(session != null){

                        ctx.pipeline().replace(this, "websocketHandler", WebSocketHandler(sessionHandler = session))

                        //Do the Handshake to upgrade connection from HTTP to WebSocket protocol
                        handleHandshake(ctx, msg)
                    }else if(initiationResult.response != null){
                        Translate.writeResponse(msg, ctx.channel(), initiationResult.response, responseCreator)
                    }else{
                        super.channelRead(ctx, msg)
                    }

                }
            }else{
                super.channelRead(ctx, msg)
            }
        }catch (t:Throwable){
            val channelId = ctx.channel().id()
            logs.logThrowable(t, "There was an error handling channel $channelId; closing it now")
            ctx.channel().closeFuture().addListener {
                logs.log("Channel $channelId successfully closed")
            }
        }


    }

    private fun parseCommaSeparatedHeader(v:String) = v.split(",").map { it.trim() }

    private fun getConnectionHeader(msg:HttpRequest): List<String>? {
        return msg.headers().get(HttpHeaderNames.CONNECTION)?.let(::parseCommaSeparatedHeader)
    }

    private fun getUpgradeHeader(msg:HttpRequest): List<String>? {
        return msg.headers().get(HttpHeaderNames.UPGRADE)?.let(::parseCommaSeparatedHeader)
    }

    private fun hasUpgradeHeaderValue(v:String, msg: HttpRequest):Boolean = (getUpgradeHeader(msg) ?: emptyList()).any { it.equals(v, ignoreCase = true) }
    private fun hasConnectionHeaderValue(v:String, msg: HttpRequest):Boolean = (getConnectionHeader(msg) ?: emptyList()).any { it.equals(v, ignoreCase = true) }

    private fun isWebsocketsUpgradeRequest(msg: HttpRequest):Boolean {
        println("getConnectionHeader(msg) " + getConnectionHeader(msg))
        val isUpgrade = hasConnectionHeaderValue("Upgrade", msg)
        val isToWebsockets = hasUpgradeHeaderValue("WebSocket", msg)
        return ( isUpgrade && isToWebsockets )
    }



    private fun handleHandshake(ctx: ChannelHandlerContext, req: HttpRequest) {
        val wsFactory = WebSocketServerHandshakerFactory(getWebSocketURL(req), null, true)
        handshaker = wsFactory.newHandshaker(req)
        if (handshaker == null) {
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel())
        } else {
            handshaker!!.handshake(ctx.channel(), req)
        }
    }

    private fun getWebSocketURL(req: HttpRequest) = "ws://" + req.headers()["Host"] + req.uri
}


private object EmptyReadOnlyAccumulator: ByteAccumulator {
    private val emptyBytes = ByteArray(0)
    override fun toStream() = ByteArrayInputStream(emptyBytes)
    override fun out() = throw Exception("This is ready only")
    override fun dispose(){}
}

private class WebSocketHandler(private val sessionHandler: WebSocketChannelHandler) : ChannelInboundHandlerAdapter() {

    override fun handlerRemoved(ctx: ChannelHandlerContext) {
        this.sessionHandler.handleEvent(ChannelDisconnected)
    }

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        if (msg is WebSocketFrame) {
            sessionHandler.handleEvent(FrameReceived(toHttpObjectsFrame(msg)))
        }
    }
}
