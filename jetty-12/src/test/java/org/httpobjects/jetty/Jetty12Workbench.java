package org.httpobjects.jetty;

import org.httpobjects.HttpObject;
import org.httpobjects.Request;
import org.httpobjects.Response;
import org.httpobjects.eventual.Eventual;
import org.httpobjects.eventual.Resolvable;

public class Jetty12Workbench {
    public static void main(String[] args) {

        HttpObjectsJettyHandler.launchServer(8045, new HttpObject("/"){
            @Override
            public Eventual<Response> get(Request req) {
                Resolvable<Response> r = new Resolvable<>();

                new Thread(){
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(4000);
                            r.resolve(OK(Text("Hello world")));
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }

                    }
                }.start();

                return r;
            }
        });
    }
}
