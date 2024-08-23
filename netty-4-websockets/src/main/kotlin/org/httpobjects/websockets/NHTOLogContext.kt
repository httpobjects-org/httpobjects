package org.httpobjects.websockets

class NHTOLogContext(val o:Any) {
    private val context:String by lazy {
        o.javaClass.simpleName
    }
    fun log(m:()->String){
        log(m())
    }
    fun log(m:String){
        println("[${context}] $m")
    }
    fun logThrowable(t:Throwable, m:String){
        t.printStackTrace()
        println("[${context}] ${t.javaClass.simpleName} $m")
    }
}