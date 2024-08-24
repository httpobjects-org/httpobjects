package org.httpobjects.netty4;

import org.httpobjects.HttpObject;
import org.httpobjects.Request;
import org.httpobjects.Response;
import org.httpobjects.eventual.Eventual;

public class SampleServer {
    public static void main(String[] args) {
        BasicNetty4Server.serveHttp(8000, new HttpObject("/"){
            @Override
            public Eventual<Response> get(Request req) {
                return OK(Text("hi")).resolved();
            }
        });
    }
}
