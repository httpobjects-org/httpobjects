package org.httpobjects.netty4;

import org.httpobjects.HttpObject;
import org.httpobjects.Request;
import org.httpobjects.Response;

public class SampleServer {
    public static void main(String[] args) {
        BasicNetty4Server.serveHttp(8000, new HttpObject("/"){
            @Override
            public Response get(Request req) {
                return OK(Text("hi"));
            }
        });
    }
}
