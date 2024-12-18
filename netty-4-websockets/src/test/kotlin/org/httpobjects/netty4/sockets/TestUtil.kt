package org.httpobjects.netty4.sockets

import org.httpobjects.tck.PortFinder
import org.httpobjects.websockets.NettyWithWebsockets
import org.httpobjects.websockets.WebSocketObject
import org.junit.Assert
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

fun waitForConditionOrFail(timeout:Long, condition:()->Boolean) {
    pauseForCondition(
        timeout = timeout,
        condition = condition,
        timeoutAction = {Assert.fail("Timeed-out waiting for condition ($it > $timeout)")},
    )
}
fun pauseForCondition(timeout:Long, timeoutAction:(duration:Long)->Unit = {}, condition:()->Boolean) {
    val start = System.currentTimeMillis()

    var keepGoing = true
    while(keepGoing){
        val duration = System.currentTimeMillis() - start
        if(condition()){
            keepGoing = false
        }else if(duration > timeout){
            keepGoing = false
            timeoutAction(duration)
        }else{
            Thread.sleep(10)
        }
    }

}



private val enoughTimeToBeSureNothingElseIsGoingToHappen:Long = 1000

fun waitUntilPrettySureThisIsntGoingToHappen(condition:()->Boolean){
    pauseForCondition(timeout = enoughTimeToBeSureNothingElseIsGoingToHappen){
        condition()
    }
    if(condition()){
        Assert.fail("This wasn't supposed to happen, but it did: $condition")
    }
}



fun runWithSocketsObject(o:WebSocketObject, fn:(port:Int)->Unit){
    val port = PortFinder.allocateFreePort(null).port
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

fun InputStream.lines():Sequence<String> {
    val reader = BufferedReader(InputStreamReader(this))

    return generateSequence{
        reader.readLine()
    }
}
