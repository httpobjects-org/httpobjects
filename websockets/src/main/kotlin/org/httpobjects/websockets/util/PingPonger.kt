package org.httpobjects.websockets.util

import org.httpobjects.websockets.*
import java.nio.charset.Charset
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class PingPonger(
    private val pingInterval:Long? = null,
    private val executor:ScheduledExecutorService,
    private val context:WebSocketChannel){

    private data class PingPong(val id:Long, val received:Boolean)

    private var nextPing:ScheduledFuture<*>? = null
    private var nextVerify:ScheduledFuture<*>? = null
    private var latestPingPong: PingPong? = null
    private var lastActivityTimestamp:Long = System.currentTimeMillis()

    init {
        scheduleNextPing()
    }

    private fun log(m:String){
        println("[channel-${context.id()}] $m")
    }

    fun lastActivity() = lastActivityTimestamp

    fun stop(){
        log("Stopping")
        listOfNotNull(nextVerify, nextPing).forEach{
            it.cancel(true)
        }
    }

    private fun scheduleNextPing(){
        if(pingInterval!=null){
            log("Scheduling ping")
            nextPing?.cancel(false)
            nextPing = executor.schedule(::sendNextPing, pingInterval, TimeUnit.MILLISECONDS)
        }
    }

    private fun verify(id:Long){
        val p = this.latestPingPong
        if(p==null){
            throw Exception("OOPS!")
        }else{
            if(p.id <= id && !p.received){
                log("Client took too long to respond ... closing")
                context.close()
            }
        }
    }

    private fun scheduleNextVerify(id:Long){
        if(pingInterval!=null){
            log("Scheduling Verify")
            nextVerify?.cancel(false)
            nextVerify = executor.schedule({verify(id)}, pingInterval, TimeUnit.MILLISECONDS)
        }
    }

    private fun sendNextPing(){
        val currentId = latestPingPong?.id ?:0
        val nextId = currentId + 1
        val label = "$nextId"

        log("Sending ping $nextId/$label")
        
        this.latestPingPong = PingPong(
            id = nextId,
            received = false)

        context.writeAndFlush(PingWebSocketFrame.of(label.encodeToByteArray())).then {
            log("Sent ping $nextId")
            scheduleNextVerify(nextId)
        }
    }


    fun handleEvent(event: WebSocketChannelEvent, nonControlFrameReceived:(WebSocketChannelEvent)->Unit){
        scheduleNextPing()

        when(event){
            is ChannelConnected -> {}
            is ChannelDisconnected -> {
                nextPing?.cancel(false)
                nonControlFrameReceived(event)
            }
            is FrameReceived -> {
                lastActivityTimestamp = System.currentTimeMillis()
                val frame = event.frame
                val channel = context
                when(frame){
                    is PingWebSocketFrame -> {

                       log("Got ping, $frame")
                        val content = frame.data()
                        channel.writeAndFlush(PongWebSocketFrame.of(content.arrayCopy())).then{
                            log("Sent pong")
                        }
                    }
                    is PongWebSocketFrame -> {
                        val id = frame.data().toString(Charset.forName("UTF8")).toLong()
                        val latestPingPong = this.latestPingPong
                        if(latestPingPong == null){
                            log("Warning: got a pong but there was no ping")
                        }else if(latestPingPong.id != id){
                           log("Warning: the ids didn't match!  Closing session $context")
                            channel.close()
                        }else{
                            log("Correct pong received - session will live to fight another day: $context")
                            this.latestPingPong = latestPingPong.copy(received = true)
                            scheduleNextPing()
                        }
                    }
                    else -> nonControlFrameReceived(event)
                }
            }
        }

    }
}