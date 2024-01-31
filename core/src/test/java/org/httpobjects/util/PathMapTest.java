package org.httpobjects.util;

import org.httpobjects.HttpObject;
import org.httpobjects.path.RegexPathPattern;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import static org.httpobjects.test.TestUtils.assertListOfSameItems;

public class PathMapTest {

    @Test
    public void happyPath(){
        // given
        final HttpObject a = new HttpObject("/bar");
        final HttpObject b = new HttpObject("/bar/{foo}");

        final List<PathMapEntry> routes = Arrays.asList(
            new PathMapEntry("/bar", a),
            new PathMapEntry("/bar/{foo}", b)
        );

        // when
        final List<HttpObject> items = PathMap.resolveNoShadows(routes);

        // then
        assertListOfSameItems(Arrays.asList(a, b), items);
    }

    @Test
    public void builderHappyPath(){
        // given
        final HttpObject a = new HttpObject("/bar");
        final HttpObject b = new HttpObject("/bar/{foo}");

        // when
        final List<HttpObject> items = PathMap.start()
            .with("/bar", a)
            .with("/bar/{foo}", b)
            .resolveNoShadows();

        // then
        assertListOfSameItems(Arrays.asList(a, b), items);
    }


    @Test
    public void nestedHappyPath(){
        // given
        final HttpObject a = new HttpObject("/api/members/{member-id}/cards");
        final HttpObject b = new HttpObject("/api/members/{member-id}/cards/{card-id}");
        final List<PathMapEntry> routes = Arrays.asList(
            new PathMapEntry("/api/members/{member-id}/cards", a),
            new PathMapEntry("/api/members/{member-id}/cards/{card-id}", b)
        );

        // when
        final List<HttpObject> items = PathMap.resolveNoShadows(routes);

        // then
        assertListOfSameItems(Arrays.asList(a, b), items);
    }


    @Test
    public void badPath(){
        // given
        final List<PathMapEntry> routes = Arrays.asList(
            new PathMapEntry("/bar", new HttpObject("/foo"))
        );

        // when
        List<HttpObject> items = null;
        Throwable t = null;
        try{
            items = PathMap.resolveNoShadows(routes);
        }catch (Throwable err){
            t = err;
        }

        // then
        Assert.assertNull(items);
        Assert.assertNotNull(t);
        Assert.assertEquals("Expected /bar but was /foo", t.getMessage());

    }

    @Test
    public void rejectsShadowedPaths(){
        // given
        final List<PathMapEntry> routes = Arrays.asList(
            new PathMapEntry("/api/members/{member-id}/cards/{card-id}", new HttpObject("/api/members/{member-id}/cards/{card-id}")),
            new PathMapEntry("/api/members/{member-id}/cards", new HttpObject("/api/members/{member-id}/cards"))
        );

        // when
        Throwable t;
        try{
            PathMap.resolveNoShadows(routes);
            t = null;
        }catch (Throwable err){
            t = err;
        }

        // then
        Assert.assertNotNull(t);
        Assert.assertEquals("/api/members/{member-id}/cards is shadowed by /api/members/{member-id}/cards/{card-id}", t.getMessage());

    }

    @Test
    public void customPatternsHappyPath(){
        // given
        final HttpObject a = new HttpObject(new RegexPathPattern(Pattern.compile("/foo")));
        final HttpObject b = new HttpObject(new RegexPathPattern(Pattern.compile("/bar")));
        final List<PathMapEntry> routes = Arrays.asList(
            new PathMapEntry("/foo", a),
            new PathMapEntry("/bar", b)
        );

        // when
        final List<HttpObject> result = PathMap.resolveNoShadows(routes);

        // then
        assertListOfSameItems(Arrays.asList(a, b), result);

    }

    @Test
    public void customPatternsSadPath(){
        // given
        final HttpObject a = new HttpObject(new RegexPathPattern(Pattern.compile("/foo")));
        final HttpObject b = new HttpObject(new RegexPathPattern(Pattern.compile("/bar")));
        final List<PathMapEntry> routes = Arrays.asList(
            new PathMapEntry("/poo", a),
            new PathMapEntry("/bar", b)
        );

        // when
        Throwable t;
        try{
            PathMap.resolveNoShadows(routes);
            t = null;
        }catch (Throwable err){
            t = err;
        }

        // then
        Assert.assertNotNull(t);
        Assert.assertEquals("Expected /poo but was /foo", t.getMessage());

    }


}
