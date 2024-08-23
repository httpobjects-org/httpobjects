package org.httpobjects.websockets

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelInitializer
import io.netty.channel.EventLoopGroup
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.http.HttpRequestDecoder
import io.netty.handler.codec.http.HttpResponseEncoder
import io.netty.handler.ssl.SslContext
import jdk.jfr.Event
import org.httpobjects.ErrorHandler
import org.httpobjects.HttpObject
import org.httpobjects.netty4.BasicLog
import org.httpobjects.netty4.HttpObjectsResponder
import org.httpobjects.netty4.ResponseCreationStrategy
import org.httpobjects.netty4.buffer.ByteAccumulatorFactory

interface NettyWithWebsocketsServer{
    fun stop():EventualResult<Unit>
}
object NettyWithWebsockets {
    private val log = NHTOLogContext(this)

    fun serve(
        port: Int,
        objects: List<HttpObject>,
        websocketsSessionHandlers:List<WebSocketObject>,
        responseStrategy: ResponseCreationStrategy,
        buffers: ByteAccumulatorFactory,
        ssl: SslContext?,
        errorHandler:ErrorHandler): NettyWithWebsocketsServer {


        val httpobjectsRequestHandler = HttpObjectsResponder(objects, errorHandler)
        val bossGroup: EventLoopGroup = NioEventLoopGroup()
        val workerGroup: EventLoopGroup = NioEventLoopGroup()
        val log = object:BasicLog("netty"){
            override fun emit(formattedMessage: String?) {
                log.log(formattedMessage ?: "")
            }
        }
        val b = ServerBootstrap()
        b.group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel::class.java)
            //.handler(new LoggingHandler(LogLevel.INFO))
            .childHandler(object : ChannelInitializer<Channel>() {
                override fun initChannel(ch: Channel) {
                    val p = ch.pipeline()
                    if (ssl != null) {
                        p.addLast(ssl.newHandler(ch.alloc()))
                    }
                    p.addLast(HttpRequestDecoder())
                    p.addLast(HttpResponseEncoder())
                    p.addLast(HttpObjectsPlusWebsocketsHandler(websocketsSessionHandlers, httpobjectsRequestHandler, responseStrategy, buffers, log))
//                    p.addLast(HttpobjectsChannelHandler(httpobjectsRequestHandler, buffers))
                }
            })

        val bindFuture = b.bind(port).sync()

        return object:NettyWithWebsocketsServer{
            override fun stop():EventualResult<Unit> {
                val result = BasicEventualResult<Unit>()

                val actions = listOf(
                    bossGroup.shutdownGracefully(),
                    workerGroup.shutdownGracefully(),
                    bindFuture.channel().closeFuture(),
                )

                actions.forEach {
                    it.addListener {
                        if(actions.all { it.isDone }){
                            result.resolve(Unit)
                        }
                    }
                }


                return result
            }

        }
    }
}

