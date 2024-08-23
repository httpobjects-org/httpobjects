package org.httpobjects.eventual;

import java.util.ArrayList;
import java.util.List;

public class BasicEventualResult<T> implements EventualResult<T> {
    private List<ResultHandler<T>> listeners  = new ArrayList<ResultHandler<T>>();
    private T resolution;
    private boolean hasResolved = false;

    public BasicEventualResult(T resolution) {
        this();
        this.resolve(resolution);
    }
    public BasicEventualResult() {
        System.out.println("created " + this);
    }

    @Override
    public void then(ResultHandler<T> fn) {

        if(hasResolved){
            fn.exec(this.resolution);
        }else{
            listeners.add(fn);
        }

    }

    @Override
    public T join() {
        synchronized (this){
            if(!this.hasResolved){
                try{
                    System.out.println("Waiting for result of " + this + "...");
                        this.wait();
                    System.out.println("Done waiting...");

                    return resolution;
                }catch(Throwable t){
                    throw new RuntimeException(t);
                }
            }else{
                return this.resolution;
            }
        }
    }

    public void resolve(T result){
        synchronized (this){
            System.out.println("resolve() " + this);
            this.resolution = result;
            this.hasResolved = true;

            for(ResultHandler<T> listener : this.listeners){
                listener.exec(result);
                this.listeners.remove(listener);
            }

            this.notifyAll();
        }
    }
}