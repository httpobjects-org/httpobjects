package org.httpobjects.eventual;

import org.junit.Test;

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
}
