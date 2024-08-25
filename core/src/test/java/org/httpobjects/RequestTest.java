package org.httpobjects;

import org.httpobjects.header.GenericHeaderField;
import org.httpobjects.header.request.RequestHeader;
import org.httpobjects.path.Path;
import org.httpobjects.util.Method;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.*;

public class RequestTest {

    @Test
    public void bodyUtf8StringUnbounded() throws Exception {
        // given
        Request req1 = new Request() {
            @Override public Query query() { return new Query("?foo=bar"); }
            @Override public Path path() { return new Path("/foo/bar/"); }
            @Override public RequestHeader header() {
                return new RequestHeader(
                        new GenericHeaderField("foocience", "foo"),
                        new GenericHeaderField("bariness", "bar")
                );
            }
            @Override public ConnectionInfo connectionInfo() {
                return new ConnectionInfo("10.10.10.10", 40, "20.20.20.20", 80);
            }
            @Override public boolean hasRepresentation() { return true; }
            @Override public Representation representation() { return DSL.Text("foo bar"); }
            @Override public Request immutableCopy() { return this; }
            @Override public Method method() { return Method.POST; }
        };
        
        // when
        Optional<String> result1 = req1.bodyUtf8StringUnbounded();

        // then
        assertEquals(Optional.of("foo bar"), result1);
    }


    @Test
    public void bodyUtf8StringUnboundedTestEmptyOptional() throws Exception {
        // given
        Request req2 = new Request() {
            @Override public Query query() { return new Query("?foo=bar"); }
            @Override public Path path() { return new Path("/foo/bar/"); }
            @Override public RequestHeader header() {
                return new RequestHeader(
                        new GenericHeaderField("foocience", "foo"),
                        new GenericHeaderField("bariness", "bar")
                );
            }
            @Override public ConnectionInfo connectionInfo() {
                return new ConnectionInfo("10.10.10.10", 40, "20.20.20.20", 80);
            }
            @Override public boolean hasRepresentation() { return false; }
            @Override public Representation representation() { return null; }
            @Override public Request immutableCopy() { return this; }
            @Override public Method method() { return Method.GET; }
        };

        // when
        Optional<String> result3 = req2.bodyUtf8StringUnbounded();

        // then
        assertEquals(Optional.<String>empty(), result3);
    }


}
