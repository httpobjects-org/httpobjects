package org.httpobjects.websockets

import io.netty.bootstrap.Bootstrap
import io.netty.channel.*
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.http.DefaultHttpHeaders
import io.netty.handler.codec.http.FullHttpResponse
import io.netty.handler.codec.http.HttpClientCodec
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.websocketx.*
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame
import io.netty.handler.codec.http.websocketx.WebSocketFrame
import io.netty.handler.ssl.SslContext
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import io.netty.util.CharsetUtil
import org.httpobjects.websockets.*
import java.net.URI


/**
 * Generic JVM code to connect to a websockets server/endpoint and send/receive frames
 * Taken from https://github.com/netty/netty/blob/4.0/example/src/main/java/io/netty/example/http/websocketx/client/WebSocketClientHandler.java
 */
class NettyWebSocketClient(private val group: EventLoopGroup):WebSocketClient{

    private fun defaultPortForScheme(scheme:String) = when(scheme.toLowerCase()){
        "ws" -> 80
        "wss" -> 443
        else -> null
    }

    override fun <T:WebSocketChannelHandler>connect(url:String, headers:Map<String, String>, onDisconnect:(T)->Unit, beginSession:(context: WebSocketChannel)->T):T? {

        val uri = URI(url)
        val scheme = if (uri.scheme == null) "ws" else uri.scheme
        val host = if (uri.host == null) "127.0.0.1" else uri.host
        val port: Int = if (uri.port == -1) (defaultPortForScheme(scheme) ?: -1) else uri.port

        if(!setOf("ws", "wss").contains(scheme.toLowerCase())) throw Exception("Only WS(S) is supported.")

        val ssl = "wss".equals(scheme, ignoreCase = true)

        val sslCtx: SslContext? = if (ssl) {
            SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build()
        } else {
            null
        }

        // Connect with V13 (RFC 6455 aka HyBi-17). You can change it to V08 or V00.
        // If you change it to V00, ping is not supported and remember to change
        // HttpResponseDecoder to WebSocketHttpResponseDecoder in the pipeline.

        val headerFu = DefaultHttpHeaders()

        headers.forEach{name, value ->
            headerFu.add(name, value)
        }


        val handler = NettyWebSocketClientHandler(
            beginSession = {ctx->
                beginSession(toHttpObjectsChannel(ctx))
            },
            onDisconnect = onDisconnect,
            WebSocketClientHandshakerFactory.newHandshaker(
                uri,
                WebSocketVersion.V13,
                null,
                false,
                headerFu))

        val b = Bootstrap()
        b.group(group)
            .channel(NioSocketChannel::class.java)
            .handler(object : ChannelInitializer<SocketChannel?>() {
                override fun initChannel(ch: SocketChannel?) {
                    val p = ch!!.pipeline()
                    if (sslCtx != null) {
                        p.addLast(sslCtx.newHandler(ch.alloc(), host, port))
                    }
                    p.addLast(
                        HttpClientCodec(),
                        HttpObjectAggregator(8192),
                        handler
                    )
                }

            })


        b.connect(uri.host, port).sync().channel()

        handler.handshakeFuture()!!.sync()

        return handler.session()
    }
}


class NettyWebSocketClientHandler<T:WebSocketChannelHandler>(
    private val beginSession:(context: ChannelHandlerContext)->T?,
    private val onDisconnect:(T)->Unit = {},
    private val handshaker: WebSocketClientHandshaker):SimpleChannelInboundHandler<Any?>() {

    private var handshakeFuture: ChannelPromise? = null
    private var session:T? = null

    private val logs = NHTOLogContext(this)

    fun session():T? = this.session

    fun handshakeFuture(): ChannelFuture? {
        return handshakeFuture
    }

    override fun handlerAdded(ctx: ChannelHandlerContext) {
        handshakeFuture = ctx.newPromise()
        session = beginSession(ctx)
    }

    override fun channelActive(ctx: ChannelHandlerContext) {
        handshaker.handshake(ctx.channel())
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        logs.log{"WebSocket Client disconnected!"}

        val session = this.session
        if(session!=null){
            session.handleEvent(ChannelDisconnected)
            this.session = null
            onDisconnect(session)
        }else{
            throw Exception("Something is wrong ... I should have had a session, but there ain't one :(")
        }
    }

    override fun channelRead0(ctx: ChannelHandlerContext, msg: Any?) {
        val ch = ctx.channel()
        if (!handshaker.isHandshakeComplete) {
            try {
                handshaker.finishHandshake(ch, msg as FullHttpResponse)
                logs.log{"WebSocket Client connected!"}
                handshakeFuture!!.setSuccess()
            } catch (e: WebSocketHandshakeException) {
                logs.log{"WebSocket Client failed to connect"}
                handshakeFuture!!.setFailure(e)
            }
            return
        }
        if (msg is FullHttpResponse) {
            throw IllegalStateException(
                "Unexpected FullHttpResponse (getStatus=" + msg.status +
                        ", content=" + msg.content().toString(CharsetUtil.UTF_8) + ')'
            )
        }
        val frame = msg as WebSocketFrame
        if (frame is CloseWebSocketFrame) {
            logs.log{"WebSocket Client received closing"}
            ch.close()
        }else{
            session?.handleEvent(FrameReceived(toHttpObjectsFrame(frame)))
        }
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        cause.printStackTrace()
        if (!handshakeFuture!!.isDone) {
            handshakeFuture!!.setFailure(cause)
        }
        ctx.close()
    }
}