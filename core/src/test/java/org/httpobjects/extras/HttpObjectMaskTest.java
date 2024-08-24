package org.httpobjects.extras;

import org.httpobjects.*;
import org.httpobjects.header.request.RequestHeader;
import org.httpobjects.path.Path;
import org.httpobjects.util.HttpObjectUtil;
import org.httpobjects.util.Method;
import org.junit.Assert;
import org.junit.Test;

import static org.httpobjects.DSL.*;
import static org.junit.Assert.*;

public class HttpObjectMaskTest {

    @Test
    public void maskShouldUseThePathToSelectBetweenResources() throws Exception {
        // given
        Response leftResponse = OK(Text("left response"));
        Response rightResponse = OK(Text("right response"));
        HttpObject left = new HttpObject("/left", leftResponse);
        HttpObject right = new HttpObject("/right", rightResponse);
        HttpObject masked = HttpObjectMask.mask(left, right);
        Request leftRequest = request("/left", Method.GET);
        Request rightRequest = request("/right", Method.GET);

        // when
        Response maybeLeftResponse = masked.get(leftRequest).join();
        Response maybeRightResponse = masked.get(rightRequest).join();

        // then
        assertEquals(leftResponse, maybeLeftResponse);
        assertEquals(rightResponse, maybeRightResponse);
    }

    @Test
    public void maskShouldReturnNOT_FOUNDWhenPathMatchingFailsOnBothResources() throws Exception {
        // given
        Response leftResponse = OK(Text("left response"));
        Response rightResponse = OK(Text("right response"));
        HttpObject left = new HttpObject("/left", leftResponse);
        HttpObject right = new HttpObject("/right", rightResponse);
        Representation repr = Text("Not Found");
        HttpObject masked = HttpObjectMask.mask(left, right, repr);
        Request request = request("/middle", Method.GET);

        // when
        Response maybe404 = masked.get(request).join();

        // then
        Assert.assertEquals(ResponseCode.NOT_FOUND, maybe404.code());
        assertEquals("Not Found",
                HttpObjectUtil.toUtf8(maybe404.representation()));
    }

    private Request request(String path, Method method) {
        return new Request() {
            @Override public Query query() { return new Query(""); }
            @Override public Path path() { return new Path(path); }
            @Override public RequestHeader header() { return new RequestHeader(); }
            @Override public ConnectionInfo connectionInfo() {
                return new ConnectionInfo("10.10.10.10", 40,
                        "20.20.20.20", 80);
            }
            @Override public boolean hasRepresentation() { return false; }
            @Override public Representation representation() { return null; }
            @Override public Request immutableCopy() { return this; }
            @Override public Method method() { return method; }
        };
    }

}
