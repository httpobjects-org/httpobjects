package org.httpobjects.util;

import org.httpobjects.HttpObject;
import org.httpobjects.impl.fn.FunctionalJava;
import org.httpobjects.path.PathParamName;
import org.httpobjects.path.SimplePathPattern;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

public class ShadowCheck {

    public static FailableResult<List<HttpObject>> shadowingErrors(final List<HttpObject> objects){
        final List<String> errors = new ArrayList<String>();

        for(int i = 0; i < objects.size(); i++){
            final HttpObject route = objects.get(i);
            final List<HttpObject> priors = new ArrayList<HttpObject>();

            for(int x = 0; x < i; x++){
                priors.add(objects.get(x));
            }

            for(HttpObject prior:priors){
                if(shadows(prior, route )){
                    errors.add(route.pattern().raw() + " is shadowed by " + prior.pattern().raw());
                }
            }

        }

        if(errors.isEmpty()){
            return FailableResult.success(objects);
        }else{
            return FailableResult.failure(errors);
        }
    }
    public static List<HttpObject> assertNoShadowing(final List<HttpObject> objects){
        return shadowingErrors(objects).getOrThrow();
    }
    private static String testUrlFor(SimplePathPattern p){
        String url = p.raw();
        for(PathParamName n : p.varNames()){
            final String name = n.toString();
            url = url.replaceAll(Pattern.quote("{" + name + "}"), name);
        }
        return url;
    }
    private static boolean shadows(HttpObject prior, HttpObject route){
        SimplePathPattern p = cast(prior.pattern(), SimplePathPattern.class);
        SimplePathPattern r = cast(route.pattern(), SimplePathPattern.class);

        if(p!=null && r!=null){

            final String url = testUrlFor(r);
//            System.out.println("Created " + url + " from " + p.raw() + " and testing match against " + route.pattern());

            return p.matches(url);

        }else{
            System.out.println("WARN: can't verify that these don't clash: " + prior.pattern() + " and " + route.pattern());
            return false;
        }

    }

    private static <T> T cast(Object o, Class<T> clazz){
        if(clazz.isInstance(o)){
            return (T) o;
        }else{
            return null;
        }
    }
}
