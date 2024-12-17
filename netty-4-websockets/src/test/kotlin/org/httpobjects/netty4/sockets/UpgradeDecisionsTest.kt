package org.httpobjects.netty4.sockets

import io.netty.channel.EventLoopGroup
import io.netty.channel.nio.NioEventLoopGroup
import org.httpobjects.Request
import org.httpobjects.path.SimplePathPattern
import org.httpobjects.tck.PortFinder
import org.httpobjects.websockets.ChannelConnected
import org.httpobjects.websockets.ChannelDisconnected
import org.httpobjects.websockets.NettyWebSocketClient
import org.httpobjects.websockets.NettyWithWebsockets
import org.httpobjects.websockets.WebSocketChannel
import org.httpobjects.websockets.WebSocketChannelEvent
import org.httpobjects.websockets.WebSocketChannelHandler
import org.httpobjects.websockets.WebSocketInitiationResponse
import org.httpobjects.websockets.WebSocketObject
import org.junit.Test

class UpgradeDecisionsTest {

    @Test
    fun `connection upgrades fail cleanly when the underlying object throws an exception during the decision-making process`(){

        // given

        val objectThatFailsWhenMakingDecisions = object: WebSocketObject(SimplePathPattern("/foo")){
            override fun beginSession(request: Request, channel: WebSocketChannel,): WebSocketInitiationResponse {
                throw Exception("something happened")
            }
        }

        runWithSocketsObject(objectThatFailsWhenMakingDecisions){port ->

            // when
            val session = tryOrNull{
                NettyWebSocketClient(NioEventLoopGroup()).connect(
                    url = "ws://localhost:$port/foo",
                    headers = mapOf(),
                    onDisconnect = {},
                    beginSession = ::HandlerThatNeverCloses
                )!!
            }

            // then  - the session should have either cleanly failed or closed
            waitForConditionOrFail(5000){
                session == null || session.didClose()
            }
            println("session correctly failed: $session")
        }
    }

    fun runWithSocketsObject(o:WebSocketObject, fn:(port:Int)->Unit){
        val port = PortFinder.allocateFreePort(this).port
        val server = NettyWithWebsockets.serveSimpleHttp(port, emptyList(), listOf(o))
        try{
            fn(port)
        } finally {
            server.stop().then {
                println("Stopped")
            }
        }
    }


    fun <T> tryOrNull(fn:()->T):T? {
        return try{
            fn()
        }catch (t: Throwable){
            t.printStackTrace()
            null
        }
    }


    class HandlerThatNeverCloses(ctx:WebSocketChannel): WebSocketChannelHandler(ctx){
        private var didOpen = false
        private var didClose = false
        fun didClose() = this.didClose
        override fun handleEvent(event: WebSocketChannelEvent) {
            when(event){
                is ChannelDisconnected -> didClose = true
                is ChannelConnected -> didOpen = true
                else -> Unit
            }
        }
    }
}