package org.httpobjects.util;

import org.httpobjects.HttpObject;
import org.httpobjects.impl.fn.FunctionalJava;

import java.util.Collections;
import java.util.List;

public class FailableResult<T> {
    private final T validated;
    private final List<String> errors;

    FailableResult(T validated, List<String> errors) {
        this.validated = validated;
        this.errors = errors;
    }

    public List<String> errors(){
        if(this.errors == null) return Collections.emptyList();
        else return errors;
    }

    public T getOrThrow(){
        if(!isSuccess()){
            throw new RuntimeException(FunctionalJava.asSeq(errors).mkstring("\n").toString());
        }
        return validated;
    }

    public boolean isSuccess(){
        return errors == null;
    }

    public static <T> FailableResult<T> failure(List<String> errors){
        if(errors==null || errors.isEmpty()){
            throw new RuntimeException("Not an actual error");
        }
        return new FailableResult<T>(null, errors);
    }
    public static <T> FailableResult<T> success(T result){

        if(result==null ){
            throw new RuntimeException("Not an actual result");
        }

        return new FailableResult<T>(result, null);
    }

}