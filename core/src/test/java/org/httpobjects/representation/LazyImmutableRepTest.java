package org.httpobjects.representation;

import org.httpobjects.Representation;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import static java.nio.charset.StandardCharsets.UTF_8;

import static org.junit.Assert.*;

public class LazyImmutableRepTest {

    @Test
    public void headTest() throws Exception {
        // given
        LazyImmutableRep rep = new LazyImmutableRep(text, in("some awesome text"));

        // then
        assertEquals("some aweso", string(rep.head(10)));
        assertEquals("some ", string(rep.head(5)));
        assertEquals("some awesome te", string(rep.head(15)));
        assertEquals("some awesome text", string(rep.head(20)));
    }

    @Test
    public void getTest() throws Exception {
        // given
        LazyImmutableRep rep = new LazyImmutableRep(text, in("some meh text"));

        // then
        assertEquals("some meh text", string(rep.get()));
        assertEquals("some meh text", string(rep.get()));
    }

    @Test
    public void contentTypeTest() throws Exception {
        // given
        String contentType = String.valueOf(Math.random());
        LazyImmutableRep rep = new LazyImmutableRep(contentType, in("foo"));

        // then
        assertEquals(contentType, rep.contentType());
    }

    @Test
    public void writeTest() throws Exception {
        // given
        LazyImmutableRep rep = new LazyImmutableRep(text, in("bar"));
        ByteArrayOutputStream out1 = new ByteArrayOutputStream();
        ByteArrayOutputStream out2 = new ByteArrayOutputStream();

        // when
        rep.write(out1);
        rep.write(out2);

        // then
        assertEquals("bar", string(out1.toByteArray()));
        assertEquals("bar", string(out2.toByteArray()));
    }

    private static String text = "text/plain";

    private static InputStream in(String str) {
        return new ByteArrayInputStream(str.getBytes(UTF_8));
    }

    private static String string(byte[] bytes) {
        return new String(bytes, UTF_8);
    }
}
