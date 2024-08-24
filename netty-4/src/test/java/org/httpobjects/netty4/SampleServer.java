package org.httpobjects.netty4;

import org.httpobjects.Request;
import org.httpobjects.Response;
import org.httpobjects.migrate.SyncHttpObject;

public class SampleServer {
    public static void main(String[] args) {
        BasicNetty4Server.serveHttp(8000, new SyncHttpObject("/"){
            @Override
            public Response getSync(Request req) {
                return OK(Text("hi"));
            }
        });
    }
}
