package org.httpobjects.eventual;

public interface Eventual<T> {
    void then(ResultHandler<T> fn);

    T join();

    interface ResultHandler<T> {
          void exec(T r);
     }

    public static <T> Eventual<T> resolved(T resolution){
        final Resolvable<T> r = new Resolvable<T>();

        r.resolve(resolution);

        return r;
    }
}

