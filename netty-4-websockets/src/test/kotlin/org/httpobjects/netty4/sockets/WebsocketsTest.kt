package org.httpobjects.netty4.sockets

import org.httpobjects.Request
import org.httpobjects.path.SimplePathPattern
import org.httpobjects.websockets.WebSocketChannel
import org.httpobjects.websockets.WebSocketChannelEvent
import org.httpobjects.websockets.WebSocketChannelHandler
import org.httpobjects.websockets.WebSocketInitiationResponse
import org.httpobjects.websockets.WebSocketObject
import org.junit.Assert
import org.junit.Test
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import javax.net.SocketFactory

class WebsocketsTest {

    @Test
    fun `upgrade happy path`(){
        // given
        runWithSocketsObject(alwaysUpgradeNeverClose("/foo")){ port ->

            val socket = SocketFactory.getDefault().createSocket("localhost", port)
            val request = """
                |GET /foo HTTP/1.1
                |Host: localhost
                |Origin: localhost
                |Connection: upgrade
                |Upgrade: Websocket
            """.trimMargin().replace(Regex("\r?\n"), "\r\n") + "\r\n\r\n"

            println("Writing:\n------------\n$request\n-----------")
            socket.outputStream.write(request.encodeToByteArray())
            socket.outputStream.flush()

            val response = parseResponse(socket.inputStream)
            socket.close()

            Assert.assertTrue(response.statusLine.startsWith("HTTP/1.1 101 "))

            Assert.assertEquals(listOf("websocket"), response.headerValue("upgrade"))
            Assert.assertEquals(listOf("upgrade"), response.headerValue("connection"))

        }
    }


    @Test
    fun `upgrade with multiple Connection values`(){
        // given
        runWithSocketsObject(alwaysUpgradeNeverClose("/bar")){ port ->

            val socket = SocketFactory.getDefault().createSocket("localhost", port)
            val request = """
                |GET /bar HTTP/1.1
                |Host: localhost
                |Origin: localhost
                |Connection: keep-alive, Upgrade
                |Upgrade: Websocket
            """.trimMargin().replace(Regex("\r?\n"), "\r\n") + "\r\n\r\n"

            println("Writing:\n------------\n$request\n-----------")
            socket.outputStream.write(request.encodeToByteArray())
            socket.outputStream.flush()

            val response = parseResponse(socket.inputStream)
            socket.close()

            Assert.assertTrue(response.statusLine.startsWith("HTTP/1.1 101 "))


            Assert.assertEquals(listOf("websocket"), response.headerValue("upgrade"))
            Assert.assertEquals(listOf("upgrade"), response.headerValue("connection"))

        }
    }

    fun alwaysUpgradeNeverClose(path:String) = object: WebSocketObject(SimplePathPattern(path)){
        override fun beginSession(request: Request, channel: WebSocketChannel,) = WebSocketInitiationResponse.startSession(object:WebSocketChannelHandler(channel){
            override fun handleEvent(event: WebSocketChannelEvent) {

            }
        })
    }
}