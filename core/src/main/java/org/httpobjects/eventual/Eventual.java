package org.httpobjects.eventual;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public interface Eventual<T> {
    void then(Consumer<T> fn);
    <R> Eventual<R> map(Function<T, R> fn);
    <R> Eventual<R> flatMap(Function<T, Eventual<R>> fn);

    T join();

    public static <T> Eventual<T> resolved(T resolution){
        final Resolvable<T> r = new Resolvable<T>();

        r.resolve(resolution);

        return r;
    }
    public static <T> Eventual<T> resolveTo(Supplier<T> action){
        final Resolvable<T> r = new Resolvable<T>();

        r.resolve(action.get());

        return r;
    }
}

