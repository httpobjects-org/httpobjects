package org.httpobjects.websockets.test

import org.httpobjects.Request
import org.httpobjects.path.SimplePathPattern
import org.httpobjects.websockets.*
import org.junit.Assert
import org.junit.Test


abstract class WebsocketsTests {
    abstract fun client(): WebSocketClient
    abstract fun serve2(resources:List<WebSocketObject>):StoppableWebsocketsTestServer

    private fun runWithServer(resources:List<WebSocketObject>, fn:(server:StoppableWebsocketsTestServer)->Unit){
        val server = serve2(resources)
        try{
            fn(server)
        }finally {
            println("Stopping server on port ${server.port()}...")
            server.stop().then {
                println("Stopped on port ${server.port()}")
            }
        }
    }

    class RecordingSocketHandler(channel: WebSocketChannel) : WebSocketChannelHandler(channel){
        val eventsReceived = mutableListOf<WebSocketChannelEvent>()
        override fun handleEvent(event: WebSocketChannelEvent) {
            println("Client got $event")
            eventsReceived.add(event.copy())
        }
    }

    @Test
    fun `text frame test`() {
        // given
        val objects = listOf(object: WebSocketObject(SimplePathPattern("/foo")){
            override fun beginSession(request: Request, channel: WebSocketChannel): WebSocketInitiationResponse {
                return WebSocketInitiationResponse.startSession(object: WebSocketChannelHandler(channel) {
                    override fun handleEvent(event: WebSocketChannelEvent) {
                        println("Server got $event")

                        if(event is FrameReceived && (event.frame as? TextWebSocketFrame)?.text() == "hello world"){
                            channel.writeAndFlush(TextWebSocketFrame.of("hello back"))
                        }
                    }
                })
            }
        })

        runWithServer(objects){ server ->

            // when
            val clientHandler = client().connect(
                url = "ws://${server.host()}:${server.port()}/foo",
                headers = emptyMap(),
                onDisconnect = {},
                beginSession = {channel ->RecordingSocketHandler(channel)},
            )!!

            clientHandler.channel.writeAndFlush(TextWebSocketFrame.of("hello world"))

            // then
            waitForCondition(timeout = 4000){
                clientHandler.eventsReceived.isNotEmpty()
            }
            Assert.assertEquals(listOf("hello back"), clientHandler.eventsReceived.map{ e -> (e as? FrameReceived)?.frame?.let{it as TextWebSocketFrame?}?.text()})

        }

    }

    @Test
    fun `binary frame test`(){
        // given
        val objects = listOf(object: WebSocketObject(SimplePathPattern("/foo")){
            override fun beginSession(request: Request, channel: WebSocketChannel): WebSocketInitiationResponse {
                return WebSocketInitiationResponse.startSession(object: WebSocketChannelHandler(channel) {
                    override fun handleEvent(event: WebSocketChannelEvent) {
                        println("Server got $event")

                        if(event is FrameReceived && (event.frame as? BinaryWebSocketFrame)?.data()?.arrayCopy().contentEquals(byteArrayOf(1, 2, 3))){
                            channel.writeAndFlush(BinaryWebSocketFrame.of(byteArrayOf(4, 5, 6)))
                        }
                    }
                })
            }
        })


        runWithServer(objects) { server ->
            // when
            val clientHandler = client().connect(
                url = "ws://${server.host()}:${server.port()}/foo",
                headers = emptyMap(),
                onDisconnect = {},
                beginSession = {channel ->RecordingSocketHandler(channel)},
            )!!

            clientHandler.channel.writeAndFlush(BinaryWebSocketFrame.of(byteArrayOf(1, 2, 3)))

            // then
            waitForCondition(timeout = 4000){
                clientHandler.eventsReceived.isNotEmpty()
            }
            Assert.assertEquals(listOf(byteArrayOf(4, 5, 6).printBytes()), clientHandler.eventsReceived.map{ e -> (e as? FrameReceived)?.frame?.let{it as BinaryWebSocketFrame?}?.data()?.arrayCopy()?.printBytes()})
        }
    }

    @Test
    fun `ping frame test`(){
        // given
        val objects = listOf(object: WebSocketObject(SimplePathPattern("/foo")){
            override fun beginSession(request: Request, channel: WebSocketChannel): WebSocketInitiationResponse {
                return WebSocketInitiationResponse.startSession(object: WebSocketChannelHandler(channel) {
                    override fun handleEvent(event: WebSocketChannelEvent) {
                        println("Server got $event")

                        if(event is FrameReceived && (event.frame as? PingWebSocketFrame)?.data()?.arrayCopy().contentEquals(byteArrayOf(1, 2, 3))){
                            channel.writeAndFlush(PingWebSocketFrame.of(byteArrayOf(4, 5, 6)))
                        }
                    }
                })
            }
        })

        runWithServer(objects) { server ->
            // when
            val clientHandler = client().connect(
                url = "ws://${server.host()}:${server.port()}/foo",
                headers = emptyMap(),
                onDisconnect = {},
                beginSession = {channel ->RecordingSocketHandler(channel)},
            )!!

            clientHandler.channel.writeAndFlush(PingWebSocketFrame.of(byteArrayOf(1, 2, 3)))

            // then
            waitForCondition(timeout = 4000){
                clientHandler.eventsReceived.isNotEmpty()
            }
            Assert.assertEquals(listOf(byteArrayOf(4, 5, 6).printBytes()), clientHandler.eventsReceived.map{ e -> (e as? FrameReceived)?.frame?.let{it as PingWebSocketFrame?}?.data()?.arrayCopy()?.printBytes()})
        }
    }



    @Test
    fun `pong frame test`(){
        // given
        val objects = listOf(object: WebSocketObject(SimplePathPattern("/foo")){
            override fun beginSession(request: Request, channel: WebSocketChannel): WebSocketInitiationResponse {
                return WebSocketInitiationResponse.startSession(object: WebSocketChannelHandler(channel) {
                    override fun handleEvent(event: WebSocketChannelEvent) {
                        println("Server got $event")

                        if(event is FrameReceived && (event.frame as? PongWebSocketFrame)?.data()?.arrayCopy().contentEquals(byteArrayOf(1, 2, 3))){
                            channel.writeAndFlush(PongWebSocketFrame.of(byteArrayOf(4, 5, 6)))
                        }
                    }
                })
            }
        })


        runWithServer(objects) { server ->
            // when
            val clientHandler = client().connect(
                url = "ws://${server.host()}:${server.port()}/foo",
                headers = emptyMap(),
                onDisconnect = {},
                beginSession = {channel ->RecordingSocketHandler(channel)},
            )!!

            clientHandler.channel.writeAndFlush(PongWebSocketFrame.of(byteArrayOf(1, 2, 3)))

            // then
            waitForCondition(timeout = 4000){
                clientHandler.eventsReceived.isNotEmpty()
            }
            Assert.assertEquals(listOf(byteArrayOf(4, 5, 6).printBytes()), clientHandler.eventsReceived.map{ e -> (e as? FrameReceived)?.frame?.let{it as PongWebSocketFrame?}?.data()?.arrayCopy()?.printBytes()})
        }
    }


    private fun ByteArray.printBytes():String {
        return this.map{b -> b.toString()}.joinToString(",")
    }

    private fun waitForCondition(timeout:Long, predicate:()->Boolean){
        val threshold = System.currentTimeMillis() + timeout
        while(!predicate() && (System.currentTimeMillis() < threshold)){
            println("Waiting...")
            Thread.sleep(100)
        }
    }

}

interface StoppableWebsocketsTestServer {
    fun host():String
    fun port():Int
    fun stop(): EventualResult<Unit>
}
