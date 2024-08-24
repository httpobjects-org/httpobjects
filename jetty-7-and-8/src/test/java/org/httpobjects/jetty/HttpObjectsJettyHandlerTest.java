package org.httpobjects.jetty;

import org.eclipse.jetty.server.Server;
import org.httpobjects.*;
import org.httpobjects.client.ApacheCommons4xHttpClient;
import org.httpobjects.client.HttpClient;
import org.httpobjects.eventual.Eventual;
import org.httpobjects.tck.PortFinder;
import org.httpobjects.util.Method;

import static org.httpobjects.DSL.Text;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class HttpObjectsJettyHandlerTest {

    @Test
    public void requestBodyShouldBeReusable() throws Exception {
        // given
        HttpObject resource = new HttpObject("/", DSL.allowed(Method.POST)) {
            @Override
            public Eventual<Response> post(Request req) {
                System.out.print(req.show());
                if (req.body().get().equals("body")) return OK(Text("We did it!")).resolved();
                else return BAD_REQUEST().resolved();
            }
        };
        int port = PortFinder.findFreePort();
        HttpClient client = new ApacheCommons4xHttpClient();

        // when
        Server server = HttpObjectsJettyHandler.launchServer(port, resource);
        try{
            Response result = client.resource("http://localhost:" + port).post(Text("body"));

            // then
            assertEquals(ResponseCode.OK, result.code());
        }finally{
            server.stop();
        }
    }
}
