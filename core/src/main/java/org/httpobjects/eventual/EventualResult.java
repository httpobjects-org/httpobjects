package org.httpobjects.eventual;

public interface EventualResult<T> {
    void then(ResultHandler<T> fn);
     interface ResultHandler<T> {
          void exec(T r);
     }
}

