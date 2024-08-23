package org.httpobjects.jetty;

import org.eclipse.jetty.server.Server;
import org.httpobjects.*;
import org.httpobjects.client.ApacheCommons4xHttpClient;
import org.httpobjects.client.HttpClient;
import org.httpobjects.tck.PortFinder;
import org.httpobjects.util.Method;

import static org.httpobjects.DSL.Text;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class HttpObjectsJettyHandlerTest {

    @Test
    public void requestBodyShouldBeReusable() throws Exception {
        // given
        HttpObject resource = new SyncHttpObject("/", DSL.allowed(Method.POST)) {
            @Override
            public Response postSync(Request req) {
                System.out.print(req.show());
                if (req.body().get().equals("body")) return OK(Text("We did it!"));
                else return BAD_REQUEST();
            }
        };
        int port = PortFinder.findFreePort();
        HttpClient client = new ApacheCommons4xHttpClient();

        // when
        Server jetty = HttpObjectsJettyHandler.launchServer(port, resource);
        try {
            Response result = client.resource("http://localhost:" + port).post(Text("body"));
            // we make a post request to this server with body "body"

            // then
            assertEquals(ResponseCode.OK, result.code());
        }finally {
            jetty.stop();
        }
    }
}
