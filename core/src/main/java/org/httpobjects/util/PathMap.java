package org.httpobjects.util;

import org.httpobjects.HttpObject;
import org.httpobjects.impl.fn.Fn;
import org.httpobjects.impl.fn.FunctionalJava;
import java.util.ArrayList;
import java.util.List;

public class PathMap {
    private final List<PathMapEntry> routes;

    PathMap() {
        this.routes = new ArrayList<PathMapEntry>();
    }

    PathMap(List<PathMapEntry> routes) {
        this.routes = routes;
    }

    public PathMap with(String expected, HttpObject object){
        final List<PathMapEntry> routes = new ArrayList<PathMapEntry>();
        routes.addAll(this.routes);
        routes.add(new PathMapEntry(expected, object));
        return new PathMap(routes);
    }


    public FailableResult<List<HttpObject>> validateNoShadows(){
        final List<HttpObject> objects = objects();
        final FailableResult<List<HttpObject>> a = validate();
        final FailableResult<List<HttpObject>> b = ShadowCheck.shadowingErrors(objects);
        if(a.isSuccess() && b.isSuccess()){
            return FailableResult.success(objects);
        }else{
            return FailableResult.failure(FunctionalJava.asSeq(a.errors()).plus(FunctionalJava.asSeq(b.errors())).toList());
        }
    }

    private List<HttpObject> objects(){
        return FunctionalJava.asSeq(routes).map(new Fn<PathMapEntry, HttpObject>() {
            public HttpObject exec(PathMapEntry in) {return in.object;}
        }).toList();
    }
    public FailableResult<List<HttpObject>> validate(){
        final  List<String> errors = new ArrayList<String>();

        for(PathMapEntry entry : routes){
            final String pattern = entry.pathPattern;
            final String actual = entry.object.pattern().raw();

            if(!actual.equals(pattern)){
                errors.add("Expected " + pattern + " but was " + actual);
            }
        }

        if(!errors.isEmpty()){
            return FailableResult.failure(errors);
        }else{


            return FailableResult.success(objects());
        }
    }

    public List<HttpObject> resolve() {
        return validate().getOrThrow();
    }
    public List<HttpObject> resolveNoShadows() {
        return validateNoShadows().getOrThrow();
    }

    public static PathMap start(){
        return new PathMap();
    }



    public static List<HttpObject> resolveNoShadows(final List<PathMapEntry> routes){
        return new PathMap(routes).resolveNoShadows();
    }

    public static FailableResult<List<HttpObject>> validateNoShadows(final List<PathMapEntry> routes){
        return new PathMap(routes).validateNoShadows();
    }


}
