package org.httpobjects.netty4.sockets

import org.junit.Assert

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