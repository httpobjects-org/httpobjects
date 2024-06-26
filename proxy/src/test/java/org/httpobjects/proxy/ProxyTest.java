/**
 * Copyright (C) 2011, 2012 Commission Junction Inc.
 * <p/>
 * This file is part of httpobjects.
 * <p/>
 * httpobjects is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 * <p/>
 * httpobjects is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with httpobjects; see the file COPYING.  If not, write to the
 * Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301 USA.
 * <p/>
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole
 * combination.
 * <p/>
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent
 * modules, and to copy and distribute the resulting executable under
 * terms of your choice, provided that you also meet, for each linked
 * independent module, the terms and conditions of the license of that
 * module.  An independent module is a module which is not derived from
 * or based on this library.  If you modify this library, you may extend
 * this exception to your version of the library, but you are not
 * obligated to do so.  If you do not wish to do so, delete this
 * exception statement from your version.
 */
package org.httpobjects.proxy;

import org.httpobjects.*;
import org.httpobjects.Representation;
import org.httpobjects.ResponseCode;
import org.httpobjects.header.DefaultHeaderFieldVisitor;
import org.httpobjects.header.GenericHeaderField;
import org.httpobjects.header.HeaderField;
import org.httpobjects.header.request.RequestHeader;
import org.httpobjects.jetty.HttpObjectsJettyHandler;
import org.httpobjects.tck.PortAllocation;
import org.httpobjects.tck.PortFinder;
import org.httpobjects.test.HttpObjectAssert;
import org.httpobjects.test.MockRequest;
import org.httpobjects.util.HttpObjectUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mortbay.jetty.Server;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.ServerSocket;

import static org.hamcrest.core.StringContains.containsString;
import static org.httpobjects.DSL.Bytes;
import static org.httpobjects.test.HttpObjectAssert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class ProxyTest {
    Server jetty;

//    public static void main(String[] args) {
//        new ProxyTest().launch();
//    }


    protected int port = -1;
    private PortAllocation portAllocation = null;

    @Before
    public void launch() {
        portAllocation = PortFinder.allocateFreePort(this);
        port = portAllocation.port;
        try{
            foo();
        }catch(Throwable t){
           throw new RuntimeException("Error starting with port allocation " + portAllocation, t);
        }
    }
    private void foo(){
        jetty = HttpObjectsJettyHandler.launchServer(port,
        			new HttpObject("/") {
		            public Response get(Request req) {
		                return OK(Text("this is the root page"));
		            }
        			},
                new HttpObject("/frog") {
                    public Response get(Request req) {
                        String response = "Kermit";
                        String q = req.query().toString();
                        if (q != null) {
                            response += q;
                        }
                        return OK(Text(response));
                    }

                    public Response patch(Request req) {
                        final String response = HttpObjectUtil.toAscii(req.representation()) + " patched";

                        return OK(Text(response));
                    }

                    @Override
                    public Response put(Request req) {
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        req.representation().write(out);
                        String textualRepresentation = new String(out.toByteArray());
                        return OK(Text(textualRepresentation));
                    }
                },
                new HttpObject("/superOptions") {
                    @Override
                    public Response options(Request req) {
                        return OK(Json(""));
                    }
                },
                new HttpObject("/requirescustomheader") {
                    @Override
                    public Response get(Request req) {
                        boolean hasIt = false;
                        for (HeaderField field : req.header().fields()) {
                            boolean found = field.accept(new DefaultHeaderFieldVisitor<Boolean>() {
                                @Override
                                public Boolean visit(GenericHeaderField custom) {
                                    return custom.name().equals("xyz123") && custom.value().equals("yes!");
                                }

                                @Override
                                protected Boolean defaultValue() {
                                    return false;
                                }
                            });

                            if (found) hasIt = true;
                        }

                        if (hasIt) {
                            return OK(Text("Found it"));
                        } else {
                            return BAD_REQUEST();
                        }
                    }
                },
                new HttpObject("/notme") {
                    public Response get(Request req) {
                        return SEE_OTHER(Location("/me"));
                    }
                },
                new HttpObject("/me") {
                    public Response get(Request req) {
                        return OK(Text("It's me!"));
                    }
                },
                new HttpObject("/setcookies") {
                    public Response get(Request req) {
                        return OK(Text("Here is a tasty cookie"), HttpObject.SetCookie("id", "1234"));
                    }
                },
                new HttpObject("/piggy") {
                    public Response get(Request req) {
                        return OK(Text("Oh, kermie!"));
                    }
                },
                new HttpObject("/echo") {
                    public Response get(Request req) {
                        return OK(Bytes(req.representation().contentType(), new byte[]{}));
                    }
                },
                new HttpObject("/noresponse") {
                    @Override
                    public Response put(Request req) {
                        return new Response(ResponseCode.NO_CONTENT, null);
                    }
                },
                new HttpObject("/characters") {
                    public Response post(Request req) {
                        ByteArrayOutputStream out = new ByteArrayOutputStream();
                        req.representation().write(out);
                        String textualRepresentation = new String(out.toByteArray());
                        if (textualRepresentation.equals("Oh, kermie!")) {
                            return SEE_OTHER(Location("/piggy"), Text(textualRepresentation));
                        } else {
                            return null;
                        }
                    }
                },
                new HttpObject("/queryStringEcho") {
                    public Response get(Request req) {
                        return OK(Text(req.query().toString()));
                    }
                },
                new HttpObject("/contentTypeEcho") {
                    public Response get(Request req) {
                        String requestContentType = req.representation().contentType();

                        return OK(Text(requestContentType == null ? "null" : requestContentType));
                    }
                },
                  new HttpObject("/headerEcho"){
                      public Response get(Request req) {
                          StringBuilder sb = new StringBuilder();

                          for (HeaderField field : req.header().fields()) {
                              sb.append(String.format("%s=%s\n",field.name(),field.value()));
                          }

                          return OK(Text(sb.toString()));
                      }
                  });
    }

    @After
    public void stop() {
        try {
            jetty.stop();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void relaysTheOriginalHostField() {

        // given
        HttpObject subject = new Proxy("http://localhost:" + port + "", "http://me.com");

        // WHEN:
        Response output = subject.get(new MockRequest(subject, "/headerEcho", new Query("?name=beforeTab%09afterTab"), new GenericHeaderField("Host", "dummy-remote-host-value")));

        // THEN:
        String representation = HttpObjectUtil.toAscii(output.representation());
        System.out.println("representation = " + representation);

        assertThat(representation, containsString("x-forwarded-host=dummy-remote-host-value"));
    }

    @Test
    public void doesntDoubleEncodeTheUrl() {

        // given
        HttpObject subject = new Proxy("http://localhost:" + port + "", "http://me.com");

        // WHEN: proxying a request for url with an encoded value in the query string
        Response output = subject.get(new MockRequest(subject, "/queryStringEcho", new Query("?name=beforeTab%09afterTab")));

        // THEN: The url should come through unmolested
        assertEquals("?name=beforeTab%09afterTab", bodyOf(output).asString());
    }

    @Test
    public void sendsNonTextContentTypes() throws Exception {

        // given
        HttpObject subject = new Proxy("http://localhost:" + port + "", "http://me.com");
        Request input = new MockRequest(subject, "/echo", utf8Bytes("hi"));

        // when
        Response output = subject.get(input);

        // then
        responseCodeOf(output).assertIs(ResponseCode.OK);
        contentTypeOf(output).assertIs("image/png");
    }

    private Representation utf8Bytes(String text)
            throws UnsupportedEncodingException {
        return Bytes("image/png", text.getBytes("UTF-8"));
    }

    @Test
    public void doesntSendBlankContentTypes() {

        // given
        HttpObject subject = new Proxy("http://localhost:" + port + "", "http://me.com");
        Request input = new MockRequest(subject, "/contentTypeEcho");

        // when
        Response output = subject.get(input);

        // then
        assertEquals("null", bodyOf(output).asString());
    }

    @Test
    public void sendsCustomHeaders() {

        // given
        HttpObject subject = new Proxy("http://localhost:" + port + "", "http://me.com");
        Request input = new MockRequest(subject, "/requirescustomheader") {
            @Override
            public RequestHeader header() {
                return new RequestHeader(new GenericHeaderField("xyz123", "yes!"));
            }
        };

        // when
        Response output = subject.get(input);

        // then
        responseCodeOf(output).assertIs(ResponseCode.OK);
    }

    @Test
    public void proxiesSetCookieHeaders() {

        // given
        HttpObject subject = new Proxy("http://localhost:" + port + "", "http://me.com");
        Request input = new MockRequest(subject, "/setcookies");

        // when
        Response output = subject.get(input);

        // then
        responseCodeOf(output).assertIs(ResponseCode.OK);
        cookiesIn(output).assertContains("id", "1234");

        HttpObjectAssert.ContentType contentType = contentTypeOf(output);
        assertTrue(contentType.isPlainTextWithEncoding("utf-8"));
        assertEquals("Here is a tasty cookie", bodyOf(output).asString());
    }

    @Test
    public void proxiesOKGets() {

        // given
        HttpObject subject = new Proxy("http://localhost:" + port + "", "http://me.com");
        Request input = new MockRequest(subject, "/frog");

        // when
        Response output = subject.get(input);

        // then
        responseCodeOf(output).assertIs(ResponseCode.OK);
        assertTrue(contentTypeOf(output).isPlainTextWithEncoding("utf-8"));
        assertEquals("Kermit", bodyOf(output).asString());
    }

    @Test
    public void proxiesQueryStrings() {

        // given
        HttpObject subject = new Proxy("http://localhost:" + port + "", "http://me.com");
        Request input = new MockRequest(subject, "/frog", new Query("?name=kermit&property=value"));

        // when
        Response output = subject.get(input);

        // then
        assertTrue(responseCodeOf(output).isOK_200());
        assertTrue(contentTypeOf(output).isPlainTextWithEncoding("utf-8"));
        assertEquals("Kermit?name=kermit&property=value", bodyOf(output).asString());
    }


    @Test
    public void proxiesOKPosts() {

        // given
        HttpObject subject = new Proxy("http://localhost:" + port + "", "http://me.com");
        Request input = new MockRequest(subject, "/characters", HttpObject.Text("Oh, kermie!"));

        // when
        Response output = subject.post(input);

        // then
        responseCodeOf(output).assertIs(ResponseCode.SEE_OTHER);
        locationHeaderOf(output).assertIs("/piggy");
        assertTrue(contentTypeOf(output).isPlainTextWithEncoding("utf-8"));
        assertEquals("Oh, kermie!", bodyOf(output).asString());
    }

    @Test
    public void proxiesOKPuts() {

        // given
        HttpObject subject = new Proxy("http://localhost:" + port + "", "http://me.com");
        Request input = new MockRequest(subject, "/frog", HttpObject.Text("Kermie"));

        // when
        Response output = subject.put(input);

        // then
        responseCodeOf(output).assertIs(ResponseCode.OK);
        assertTrue(contentTypeOf(output).isPlainTextWithEncoding("utf-8"));
        assertEquals("Kermie", bodyOf(output).asString());
    }

    @Test
    public void proxiesOKPatches() {
        // given
        HttpObject subject = new Proxy("http://localhost:" + port + "", "http://me.com");
        Request input = new MockRequest(subject, "/frog", HttpObject.Text("Kermie"));

        // when
        Response output = subject.patch(input);

        // then
        responseCodeOf(output).assertIs(ResponseCode.OK);
        assertTrue(contentTypeOf(output).isPlainTextWithEncoding("utf-8"));
        assertEquals("Kermie patched", bodyOf(output).asString());
    }

    @Test
    public void proxiesOKOptions() {
        // given
        HttpObject subject = new Proxy("http://localhost:" + port + "", "http://me.com");
        Request input = new MockRequest(subject, "/superOptions", HttpObject.Text(""));

        // when
        Response output = subject.options(input);

        // then
        responseCodeOf(output).assertIs(ResponseCode.OK);
    }

    @Test
    public void handlesTargetsWithSlashes() {

        // given
        HttpObject subject = new Proxy("http://localhost:" + port + "/", "http://me.com");
        Request input = new MockRequest(subject, "/frog");

        // when
        Response output = subject.get(input);

        // then
        responseCodeOf(output).assertIsOK_200();
        assertTrue(contentTypeOf(output).isPlainTextWithEncoding("utf-8"));
        assertTrue(bodyOf(output).equals("Kermit"));
    }

    @Test
    public void handlesRedirects() {

        // given
        HttpObject subject = new Proxy("http://localhost:" + port + "/", "http://me.com");
        Request input = new MockRequest(subject, "/notme");

        // when
        Response output = subject.get(input);

        // then

        assertEquals("", bodyOf(output).asString());
        responseCodeOf(output).assertIs(ResponseCode.SEE_OTHER);
        locationHeaderOf(output).assertIs("/me");
    }

    @Test
    public void handlesNoContent() {

        // given
        HttpObject subject = new Proxy("http://localhost:" + port + "", "http://me.com");
        Request input = new MockRequest(subject, "/noresponse", HttpObject.Text("Kermie"));

        // when
        Response output = subject.put(input);

        // then
        assertEquals("", bodyOf(output).asString());
        responseCodeOf(output).assertIs(ResponseCode.NO_CONTENT);
    }



    @Test
    public void proxyShouldRelayTheOriginSourceIPAddress(){

        //given
        HttpObject subject = new Proxy("http://localhost:" + port, "http://me.com");
        Request input = new MockRequest( new ConnectionInfo("dummy-local-ip-address",8080,"dummy-remote-ip-address",10001),
          subject, "/headerEcho", new Query(""), HttpObject.Text("does not matter"));

        // when
        Response output = subject.get(input);

        // then
        String representation = HttpObjectUtil.toAscii(output.representation());
        System.out.println("representation = " + representation);

        assertThat(representation, containsString("x-forwarded-for=dummy-remote-ip-address"));


    }
    
    @Test
    public void handlesRootRequests(){
        //given
        HttpObject subject = new Proxy("http://localhost:" + port, "http://me.com");
        Request input = new MockRequest(subject, "/");

        // when
        Response output = subject.get(input);

        // then
        String representation = HttpObjectUtil.toAscii(output.representation());
        System.out.println("representation = " + representation);

        assertEquals(representation, "this is the root page");
    }




}
