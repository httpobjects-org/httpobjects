package org.httpobjects.netty4.sockets

import io.netty.channel.nio.NioEventLoopGroup
import org.httpobjects.*
import org.httpobjects.eventual.EventualResult
import org.httpobjects.netty4.ResponseCreationStrategy
import org.httpobjects.netty4.buffer.InMemoryByteAccumulatorFactory
import org.httpobjects.tck.PortFinder
import org.httpobjects.util.Method
import org.httpobjects.websockets.*
import org.httpobjects.websockets.test.StoppableWebsocketsTestServer
import org.httpobjects.websockets.test.WebsocketsTests

class NettyWebsocketsEndToEndTest: WebsocketsTests() {
    private val client = NettyWebSocketClient(group = NioEventLoopGroup())

    override fun client() = client

    override fun serve2(resources:List<WebSocketObject>):StoppableWebsocketsTestServer{

        val portAllocation = PortFinder.allocateFreePort(this)
        val port = portAllocation.port
        println("Connecting to port $port")

        val server = NettyWithWebsockets.serve(
            port,
            objects = listOf(),
            websocketsSessionHandlers = resources,
            responseStrategy = ResponseCreationStrategy.synchronous(),
            ssl = null,
            buffers = InMemoryByteAccumulatorFactory(),
            errorHandler = object:ErrorHandler{
                override fun createErrorResponse(p0: HttpObject?, p1: Method?, p2: Throwable?): Response {
                    return DSL.INTERNAL_SERVER_ERROR(DSL.Text("Not sure what to do there..."))
                }
            },
        )

        return object:StoppableWebsocketsTestServer{
            override fun host() = "localhost"
            override fun port() = port
            override fun stop(): EventualResult<Unit> {
                return server.stop()
            }
        }
    }

}
