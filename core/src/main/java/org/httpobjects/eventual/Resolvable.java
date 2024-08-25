package org.httpobjects.eventual;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class Resolvable<T> implements Eventual<T> {
    private List<Consumer<T>> listeners  = new ArrayList<Consumer<T>>();
    private T resolution;
    private boolean hasResolved = false;

    public Resolvable() {
        debug("created ");
    }

    private void debug(String m){
//        System.out.println("[Resolvable " + this + "] " + m);
    }

    @Override
    public void then(Consumer<T> fn) {
        if(hasResolved){
            fn.accept(this.resolution);
        }else{
            listeners.add(fn);
        }
    }

    @Override
    public T join() {
        synchronized (this){
            if(!this.hasResolved){
                try{
                    debug("Waiting for result ...");
                    this.wait();
                    debug("Done waiting");

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
            debug("resolved as " + result);
            this.resolution = result;
            this.hasResolved = true;

            for(Consumer<T> listener : this.listeners){
                listener.accept(result);
            }

            listeners.clear();

            this.notifyAll();
        }
    }
}