package org.httpobjects.websockets

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelInitializer
import io.netty.channel.EventLoopGroup
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.codec.http.HttpRequestDecoder
import io.netty.handler.codec.http.HttpResponseEncoder
import io.netty.handler.ssl.SslContext
import io.netty.handler.stream.ChunkedWriteHandler
import org.httpobjects.DSL
import org.httpobjects.ErrorHandler
import org.httpobjects.HttpObject
import org.httpobjects.eventual.Eventual
import org.httpobjects.eventual.Resolvable
import org.httpobjects.impl.HTLog
import org.httpobjects.netty4.BasicLog
import org.httpobjects.netty4.HttpObjectsResponder
import org.httpobjects.netty4.ResponseCreationStrategy
import org.httpobjects.netty4.buffer.ByteAccumulatorFactory
import org.httpobjects.netty4.buffer.InMemoryByteAccumulatorFactory
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

interface NettyWithWebsocketsServer{
    fun stop(): Eventual<Unit>
}
object NettyWithWebsockets {
    private val log = HTLog(this)

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
                    p.addLast(ChunkedWriteHandler())
                    p.addLast(HttpObjectsPlusWebsocketsHandler(websocketsSessionHandlers, httpobjectsRequestHandler, responseStrategy, buffers, log))
                }
            })

        val bindFuture = b.bind(port).sync()

        return object:NettyWithWebsocketsServer{
            override fun stop(): Eventual<Unit> {
                val result = Resolvable<Unit>()

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

    fun serveSimpleHttp(port: Int, objects: List<HttpObject>, webSocketObjects: List<WebSocketObject>):NettyWithWebsocketsServer {
        val mainResponsePool = ThreadPoolExecutor(0, Int.MAX_VALUE, 60L, TimeUnit.SECONDS, SynchronousQueue())
        return serve(
            port = port,
            objects = objects,
            websocketsSessionHandlers = webSocketObjects,
            responseStrategy = ResponseCreationStrategy.async(mainResponsePool),
            buffers = InMemoryByteAccumulatorFactory(),
            ssl = null,
            errorHandler = { target, method, throwable ->
                DSL.INTERNAL_SERVER_ERROR(DSL.Text("Not sure what to do there...")).resolved()
           },
        )
    }
}

