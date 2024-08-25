package org.httpobjects.eventual;

import org.junit.Test;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;

public class ResolvableTest {

    @Test
    public void map(){
        // given
        final Resolvable<Integer> initial = new Resolvable<>();
        final Eventual<String> derived = initial.map(Object::toString);

        // when
        initial.resolve(77);

        // then
        assertEquals("77", derived.join());
    }


    @Test
    public void then(){
        // given
        AtomicReference<Integer> result = new AtomicReference<>();
        final Resolvable<Integer> initial = new Resolvable<>();

        initial.then(result::set);

        // when
        initial.resolve(77);

        // then
        assertEquals(Integer.valueOf(77), result.get());
    }


    @Test
    public void mapThen(){
        // given
        AtomicReference<String> result = new AtomicReference<>();
        final Resolvable<Integer> initial = new Resolvable<>();

        initial.map(Object::toString).then(result::set);

        // when
        initial.resolve(77);

        // then
        assertEquals("77", result.get());
    }


    // TODO: consider moving into the library ... maybe with an Executor as an argument?
    private <T> Eventual<T> delayed(T t, Long millis){
        final Resolvable<T> r2 = new Resolvable<>();

        new Thread(() -> {
            try {
                Thread.sleep(millis);
                r2.resolve(t);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }).start();;

        return r2;
    }

    @Test
    public void flatMap(){
        // given

        final Resolvable<Integer> initial = new Resolvable<>();
        final Eventual<String> derived = initial.flatMap((v) -> delayed(v.toString(), 1000L));

        // when
        initial.resolve(77);

        // then
        assertEquals("77", derived.join());
    }


    @Test
    public void resolveTo(){
        // given

        final Eventual<Integer> initial = Eventual.resolveTo(()-> 77);

        // when
        final Integer result = initial.join();

        // then
        assertEquals(Integer.valueOf(77), result);
    }


    @Test
    public void resolveToAsync(){
        // given
        final Executor executor = Executors.newCachedThreadPool();
        final Eventual<Integer> initial = Eventual.resolveAsyncTo(executor, ()-> 77);

        // when
        final Integer result = initial.join();

        // then
        assertEquals(Integer.valueOf(77), result);
    }
}
