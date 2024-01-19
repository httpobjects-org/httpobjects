package org.httpobjects.netty4;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public interface ResponseCreationStrategy {
    void doIt(Runnable fn);
    Throwable stop(Long timeoutMillis);

    static ResponseCreationStrategy synchronous(){
        return new ResponseCreationStrategy(){

            @Override
            public void doIt(Runnable fn) {
                fn.run();
            }

            @Override
            public Throwable stop(Long timeoutMillis){
                return null;
            }
        };
    }

    static ResponseCreationStrategy async(Executor responseHandlingExecutor){
        return new AsyncResponseCreationStrategy(responseHandlingExecutor);
    }
}


final class AsyncResponseCreationStrategy implements ResponseCreationStrategy {
    final Executor responseHandlingExecutor;

    AsyncResponseCreationStrategy(Executor responseHandlingExecutor) {
        this.responseHandlingExecutor = responseHandlingExecutor;
    }

    @Override
    public void doIt(Runnable fn) {
        responseHandlingExecutor.execute(fn);
    }

    @Override
    public Throwable stop(Long timeoutMillis) {
        Exception result;

        if(responseHandlingExecutor instanceof ThreadPoolExecutor){
            final ThreadPoolExecutor pool = (ThreadPoolExecutor) responseHandlingExecutor;
            pool.shutdown();
            try {
                pool.awaitTermination(timeoutMillis, TimeUnit.MILLISECONDS);
                result = null;
            } catch (InterruptedException e) {
                result = e;
            }
        }else{
            result = null;
        }

        return result;
    }
}
