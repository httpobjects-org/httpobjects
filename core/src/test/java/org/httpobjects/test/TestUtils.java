package org.httpobjects.test;

import org.junit.Assert;

import java.util.List;

public class TestUtils {

    public static <T> void assertListOfSameItems(List<T> expected, List<T> actual){
        Assert.assertEquals(expected.size(), actual.size());
        for(int x=0;x<expected.size();x++){
            final T e = expected.get(x);
            final T a = actual.get(x);
            Assert.assertTrue("Should be same instance", e == a);
        }
    }
}
