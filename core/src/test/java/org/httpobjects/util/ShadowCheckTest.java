package org.httpobjects.util;

import org.httpobjects.HttpObject;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import static org.httpobjects.test.TestUtils.assertListOfSameItems;

public class ShadowCheckTest {

    @Test
    public void happyPath(){
        // given
        final HttpObject a = new HttpObject("/api/members/{member-id}/cards");
        final HttpObject b =  new HttpObject("/api/members/{member-id}/cards/{card-id}");

        final List<HttpObject> objects = Arrays.asList(a, b);

        // when
        List<HttpObject> result = null;
        try{
            result = ShadowCheck.assertNoShadowing(objects);
        }catch (Throwable err){
            err.printStackTrace();;
        }

        // then
        Assert.assertNotNull(result);
        assertListOfSameItems(objects, result);
    }

    @Test
    public void rejectsShadowedPaths(){
        // given
        final List<HttpObject> objects = Arrays.asList(
               new HttpObject("/api/members/{member-id}/cards/{card-id}"),
                new HttpObject("/api/members/{member-id}/cards")
        );

        // when
        Throwable t;
        try{
            ShadowCheck.assertNoShadowing(objects);
            t = null;
        }catch (Throwable err){
            t = err;
        }

        // then
        Assert.assertNotNull(t);
        Assert.assertEquals("/api/members/{member-id}/cards is shadowed by /api/members/{member-id}/cards/{card-id}", t.getMessage());
    }

}
