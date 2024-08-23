package org.httpobjects.websockets

interface EventualResult<T> {
    fun then(fn:(T)->Unit)
}

class BasicEventualResult<T>:EventualResult<T>{
    private var listeners:MutableList<(T)->Unit>? = mutableListOf()

    override fun then(fn: (T) -> Unit) {
        listeners?.add(fn)
    }

    fun resolve(result:T){
        this.listeners?.forEach {
            it(result)
            this.listeners?.remove(it)
        }
    }
}