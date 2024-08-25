package org.httpobjects.eventual;

import java.util.function.Consumer;

public interface Eventual<T> {
    void then(Consumer<T> fn);

    T join();

    public static <T> Eventual<T> resolved(T resolution){
        final Resolvable<T> r = new Resolvable<T>();

        r.resolve(resolution);

        return r;
    }
}

