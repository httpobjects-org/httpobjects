package org.httpobjects.eventual;

import java.util.ArrayList;
import java.util.List;

public class BasicEventualResult<T> implements EventualResult<T> {
    private List<ResultHandler<T>> listeners  = new ArrayList<ResultHandler<T>>();

    @Override
    public void then(ResultHandler<T> fn) {
        listeners.add(fn);
    }

    public void resolve(T result){
        for(ResultHandler<T> listener : this.listeners){
            listener.exec(result);
            this.listeners.remove(listener);
        }
    }
}