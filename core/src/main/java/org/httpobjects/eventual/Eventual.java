package org.httpobjects.eventual;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public interface Eventual<T> {
    void then(Consumer<T> fn);
    <R> Eventual<R> map(Function<T, R> fn);

    T join();

    public static <T> Eventual<T> resolved(T resolution){
        final Resolvable<T> r = new Resolvable<T>();

        r.resolve(resolution);

        return r;
    }
}

