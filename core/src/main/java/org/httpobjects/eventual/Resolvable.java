package org.httpobjects.eventual;

import java.util.ArrayList;
import java.util.List;

public class Resolvable<T> implements Eventual<T> {
    private List<ResultHandler<T>> listeners  = new ArrayList<ResultHandler<T>>();
    private T resolution;
    private boolean hasResolved = false;

    public Resolvable() {
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
        if(result==null){
            throw new RuntimeException("Someone tried to resolve this as null");
        }
        synchronized (this){
            System.out.println("resolve() " + this + " as " + result);
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