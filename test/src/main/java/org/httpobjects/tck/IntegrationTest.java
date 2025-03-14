/**
 * Copyright (C) 2011, 2012 Commission Junction Inc.
 *
 * This file is part of httpobjects.
 *
 * httpobjects is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * httpobjects is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with httpobjects; see the file COPYING.  If not, write to the
 * Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole
 * combination.
 *
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
package org.httpobjects.tck;

import org.apache.http.*;
import org.apache.http.client.methods.*;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.httpobjects.*;
import org.httpobjects.data.DataSource;
import org.httpobjects.data.OutputStreamDataSource;
import org.httpobjects.eventual.Eventual;
import org.httpobjects.header.DefaultHeaderFieldVisitor;
import org.httpobjects.header.GenericHeaderField;
import org.httpobjects.header.HeaderField;
import org.httpobjects.header.request.AuthorizationField;
import org.httpobjects.header.request.Cookie;
import org.httpobjects.header.request.CookieField;
import org.httpobjects.header.request.credentials.BasicCredentials;
import org.httpobjects.header.response.SetCookieField;
import org.httpobjects.header.response.WWWAuthenticateField.Method;
import org.httpobjects.path.Path;
import org.junit.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Technology Compatibility Kit
 */
public abstract class IntegrationTest extends WireTest {

    protected abstract void serve(int port, HttpObject ... objects);
    protected abstract void stopServing();

    protected int port = -1;
    private PortAllocation portAllocation = null;

    @Before
    public void setup() {
        try {
            doIt();
        }catch (Throwable t){
            throw new RuntimeException("There was a problem starting (portAllocation = " + portAllocation + ")", t);
        }
    }
    private void doIt(){
        portAllocation = PortFinder.allocateFreePort(this);
        port = portAllocation.port;
        serve(port,
        new HttpObject("/bigfiles"){
            public Eventual<Response> post(Request req) {
                return OK(Text("received " + req.representation().data().readToMemoryUnbounded().length + " bytes")).resolved();
            }
        },
        new HttpObject("/app/inbox"){
            public Eventual<Response> post(Request req) {
                return OK(Text("Message Received")).resolved();
            }
        },
        new HttpObject("/app/inbox/abc"){
            public Eventual<Response> put(Request req) {
                return OK(req.representation()).resolved();
            }
        },
        new HttpObject("/app"){
            public Eventual<Response> get(Request req) {
                return OK(Text("Welcome to the app")).resolved();
            }
        },
        new HttpObject("/app/message"){
            public Eventual<Response> post(Request req) {
                return SEE_OTHER(Location("/app"), SetCookie("name", "frank")).resolved();
            }
        },
        new HttpObject("/nothing", null){},
        new HttpObject("/files/{chunksize}/{chunks}"){
            public Eventual<Response> get(Request req) {
                final Integer chunkSize = Integer.parseInt(req.path().valueFor("chunksize"));
                final Long numChunks = Long.parseLong(req.path().valueFor("chunks"));
                System.out.println("Generating " + numChunks + " chunks of size " + chunkSize);

                final ProgressReporter reporter = new ProgressReporter("serve write", BigInteger.valueOf(chunkSize).multiply(BigInteger.valueOf(numChunks)), null);

                final byte[] chunk = new byte[chunkSize];

                return OK(new Representation() {
                    @Override
                    public String contentType() {
                        return "application/binary";
                    }

                    @Override
                    public DataSource data() {
                        return new OutputStreamDataSource(out->{
                            try{
                                for(long x=0; x < numChunks; x++){
                                    out.write(chunk);
                                    reporter.progressMade( x * chunkSize);
                                }
                                System.out.println("Done writing response");
                            }catch (Throwable t){
                                throw new RuntimeException(t);
                            }
                        });
                    }
                }).resolved();
            }
        },
        new HttpObject("/secure"){
            public Eventual<Response> get(Request req) {
                AuthorizationField authorization = req.header().authorization();
                if(authorization!=null && authorization.parse().method()==Method.Basic){

                    BasicCredentials creds = authorization.parse().basicCredentials();
                    if(creds.user().equals("Aladdin")&& creds.password().equals("open sesame")){
                        return OK(Text("You're In!")).resolved();
                    }
                }
                return UNAUTHORIZED(BasicAuthentication("secure area"), Text("You must first log-in")).resolved();
            }
        },
        new HttpObject("/echoUrl/{id}/{name}"){
            @Override
            public Eventual<Response> get(Request req) {
                try {
                    final String query = req.query().toString();
                    return OK(Text(req.path().toString() + query)).resolved();
                } catch (Exception e) {
                    e.printStackTrace();
                    return INTERNAL_SERVER_ERROR(e).resolved();
                }
            }
        },
        new HttpObject("/echoQuery"){
            @Override
            public Eventual<Response> get(Request req) {
                final StringBuffer text = new StringBuffer();
                final Query query = req.query();
                for(String name : query.paramNames()){
                    if(text.length()>0){
                        text.append('\n');
                    }
                    text.append(name + "=" + query.valueFor(name));
                }
                return OK(Text(text.toString())).resolved();
            }
        },
        new HttpObject("/echoCookies"){
            public Eventual<Response> get(Request req) {

                final StringBuffer text = new StringBuffer();
                for(HeaderField next : req.header().fields()){
                    next.accept(new DefaultHeaderFieldVisitor<Void>(){
                        @Override
                        public Void visit(CookieField cookieField) {
                            for(Cookie cookie : cookieField.cookies()){

                                text.append(cookie.name + "=" + cookie.value);
                            }
                            return null;
                        }
                    });
                }

                return OK(Text(text.toString())).resolved();
            }
        },
        new HttpObject("/echoContentType"){
            public Eventual<Response> post(Request req) {
                final String ct = req.representation().contentType();
                return OK(Text(ct == null ? "null" : ct)).resolved();
            }
        },
        new HttpObject("/cookieSetter"){
            public Eventual<Response> get(Request req){
                return OK(
                        Text("Here are some cookies!"),
                        new SetCookieField("name", "cookie monster", "sesamestreet.com"),
                        new SetCookieField("specialGuest", "mr rogers", "mrrogers.com", "/myNeighborhood", "Wed, 13-Jan-2041 22:23:01 GMT", true),
                        new SetCookieField("oldInsecureCookie", "yes", "the90sIntranet.com", "/images/animatedGifs", "Wed, 13-Jan-1999 22:23:01 GMT", false)).resolved();
            }
        },
        new HttpObject("/subpathEcho/{subPath*}"){
            @Override
            public Eventual<Response> get(Request req) {
                return OK(Text(req.path().valueFor("subPath"))).resolved();
            }
        },
        new HttpObject("/echoHasRepresentation"){
            @Override
            public Eventual<Response> post(Request req) {
                return OK(Text(req.hasRepresentation() ? "yes" : "no")).resolved();
            }
        },
        new HttpObject("/pows/{name}/{rank}/{serialnumber}"){
            @Override
            public Eventual<Response> get(Request req) {
                final Path path = req.path();
                return OK(Text(
                        path.valueFor("rank") + " " +
                        path.valueFor("name") + ", " +
                        path.valueFor("serialnumber"))).resolved();
            }
        },
        new HttpObject("/immutablecopy/{subpath*}"){
            @Override
            public Eventual<Response> post(Request req) {
                Request r = req.immutableCopy();
                final String firstPass = toString(r);
                final String secondPass = toString(r);
                return OK(Text(secondPass)).resolved();
            }

            class LowercasedHeadersByName implements Comparator<HeaderField>{
                @Override
                public int compare(HeaderField o1, HeaderField o2) {
                    return o1.name().toLowerCase().compareTo(o2.name().toLowerCase());
                }
            }
            private <T> List<T> sorted(List<T> items, Comparator<T> comparator){
                List<T> sorted = new ArrayList<T>(items);
                Collections.sort(sorted, comparator);
                return sorted;
            }

            private String toString(Request r){
                return "URI: " + r.path().toString() + "?" + r.query().toString() + "\n" +
                        toString(r.header().fields()) +
                        r.representation().data().decodeToAsciiUnbounded();
            }
            private String toString(List<HeaderField> fields){
                StringBuffer text = new StringBuffer();
                for(HeaderField field : sorted(fields, new LowercasedHeadersByName())){
                    text.append(field.name().toLowerCase() + "=" + field.value() + "\n");
                }
                return text.toString();
            }
        },
        new HttpObject("/patchme"){
            public Eventual<Response> patch(org.httpobjects.Request req) {
                try {
                    final String input = new String(req.representation().data().readToMemoryUnbounded(), "UTF-8");
                    return OK(Text("You told me to patch!" + input)).resolved();
                } catch (UnsupportedEncodingException e) {
                    return INTERNAL_SERVER_ERROR(e).resolved();
                }
            }
        },
        new HttpObject("/connectionInfo"){
            public Eventual<Response> get(Request req) {
                final ConnectionInfo connection = req.connectionInfo();
                return OK(Text("Local " + connection.localAddress + ":" + connection.localPort + ", " +
                               "Remote " + connection.remoteAddress + ":" + connection.remotePort)).resolved();
            }
        },
        new HttpObject("/head"){
        	@Override
        	public Eventual<Response> head(Request req) {
        		return OK(Text(""), new GenericHeaderField("foo", "bar")).resolved();
        	}
        },
        new HttpObject("/options"){
            @Override
            public Eventual<Response> options(Request req) {
                return OK(Text(""), new GenericHeaderField("foo", "bar")).resolved();
            }
        });
    }

    @Test
    public void supportsHead() throws Exception {
        // given
        CloseableHttpClient client = HttpClients.createDefault();
        HttpHead request = new HttpHead("http://localhost:" + port + "/head");

        //when
        HttpResponse response = client.execute(request);

        // then

        assertEquals(200, response.getStatusLine().getStatusCode());
        assertEquals("bar", response.getFirstHeader("foo").getValue());

        EntityUtils.consume(response.getEntity());
        request.releaseConnection();
        client.close();
    }

    @Test
    public void supportsOptions() throws Exception{
        // given
        CloseableHttpClient client = HttpClients.createDefault();
        HttpOptions request = new HttpOptions("http://localhost:" + port + "/options");

        //when
        HttpResponse response = client.execute(request);

        // then

        assertEquals(200, response.getStatusLine().getStatusCode());
        assertEquals("bar", response.getFirstHeader("foo").getValue());

        EntityUtils.consume(response.getEntity());
        request.releaseConnection();
        client.close();
    }

    @Test
    public void returnsConnectionInfo() throws Exception {
        // given
        String url = "http://localhost:" + port + "/connectionInfo";

        //when
        final String result = getFrom("127.0.0.1", url);

        // then
        Pattern expectedPattern = Pattern.compile("Local 127.0.0.1:" + port + ", Remote 127.0.0.1:([0-9].*)");
        assertTrue("'" + result + " should match '" + expectedPattern,
                expectedPattern.matcher(result).matches());
    }

    @Test
    public void hasRepresentation() throws Exception {
        // given
        HttpPost request = new HttpPost("http://localhost:" + port + "/echoHasRepresentation");
        request.setEntity(new StringEntity("foo bar", "UTF-8"));

        // then/when
        assertResource(request, "yes", 200);
    }

    @Test
    public void immutableCopies() throws Exception {
        // given
        HttpPost request = new HttpPost("http://localhost:" + port + "/immutablecopy/no/mutation/allowed");
        request.setEntity(new StringEntity("foo bar", "UTF-8"));

        // Original: "Apache-HttpClient/4.5.14 (Java/17.0.14)" (would break test when running on different Java version)
        request.setHeader("User-Agent", "Apache-HttpClient");
        // JettyIntegrationTest returns this lowercased "keep-alive", the Netty tests return it with default capitalization "Keep-Alive".  Make it always lowercase.
        request.setHeader("connection", "keep-alive");

        // then/when
        assertResource(request,
                "URI: /immutablecopy/no/mutation/allowed?\n" +
                "accept-encoding=gzip,deflate\n" +
                "connection=keep-alive\n" +
                "content-length=7\n" +
                "content-type=text/plain; charset=UTF-8\n" +
                "host=localhost:" + port + "\n" +
                "user-agent=Apache-HttpClient\n" +
                "foo bar", 200);

    }

    @Test
    public void relaysContentType() throws Exception {
        // given
        HttpPost request = new HttpPost("http://localhost:" + port + "/echoContentType");
        request.setEntity(new ByteArrayEntity("foo bar".getBytes(), org.apache.http.entity.ContentType.create("foobar")));

        // then/when
        assertResource(request, "foobar", 200);
    }

    @Test
    public void parsesPathVars() throws Exception {
        // given
        HttpGet request = new HttpGet("http://localhost:" + port + "/pows/marty/private/abc123");

        // then/when
        assertResource(request, "private marty, abc123", 200);
    }

    @Test
    public void parsesSubpaths() throws Exception {
        // given
        HttpGet request = new HttpGet("http://localhost:" + port + "/subpathEcho/i/am/my/own/grandpa");

        // then/when
        assertResource(request, "i/am/my/own/grandpa", 200);
    }

    @Test
    public void supportsPatch() throws Exception {
        // given
        HttpPatch request = new HttpPatch("http://localhost:" + port + "/patchme");
        request.setEntity(new StringEntity(" foo bar", "text/plain", "UTF-8"));

        // then/when
        assertResource(request, "You told me to patch! foo bar", 200);
    }

    @Test
    public void setCookieHeadersAreTranslated() throws Exception{
        // given
        HttpGet request = new HttpGet("http://localhost:" + port + "/cookieSetter");
        CloseableHttpClient client = HttpClients.createDefault();

        // when
        HttpResponse response = client.execute(request);

        // then
        assertEquals(200, response.getStatusLine().getStatusCode());
        List<Header> setCookies = sortByValue(Arrays.asList(response.getHeaders("Set-Cookie")));
        assertEquals(3, setCookies.size());

        {
            String value = setCookies.get(0).getValue();
            SetCookieField cookie = SetCookieField.fromHeaderValue(value);
            assertEquals("name", cookie.name);
            assertEquals("cookie monster", cookie.value);
            assertEquals("sesamestreet.com", cookie.domain);
        }

        {
            String value = setCookies.get(1).getValue();
            SetCookieField cookie = SetCookieField.fromHeaderValue(value);
            assertEquals("oldInsecureCookie", cookie.name);
            assertEquals("yes", cookie.value);
            assertEquals("the90sintranet.com", cookie.domain.toLowerCase());
            assertEquals("/images/animatedGifs", cookie.path);
            assertEquals("Wed, 13-Jan-1999 22:23:01 GMT", cookie.expiration);
            assertEquals(null, cookie.secure);
        }

        {
            String value = setCookies.get(2).getValue();
            SetCookieField cookie = SetCookieField.fromHeaderValue(value);
            assertEquals("specialGuest", cookie.name);
            assertEquals("mr rogers", cookie.value);
            assertEquals("mrrogers.com", cookie.domain);
            assertEquals("/myNeighborhood", cookie.path);
            assertEquals("Wed, 13-Jan-2041 22:23:01 GMT", cookie.expiration);
            assertEquals(Boolean.TRUE, cookie.secure);
        }

        EntityUtils.consume(response.getEntity());
        request.releaseConnection();
        client.close();
    }
    private List<Header> sortByValue(final List<Header> cookies) {
        List<Header> result = new ArrayList<Header>(cookies);
        Collections.sort(result, new Comparator<Header>() {
            @Override
            public int compare(Header o1, Header o2) {
                return o1.getValue().compareTo(o2.getValue());
            }
        });
        return result;
    }



    @Test
    public void requestCookiesAreTranslated() throws Exception {
        // WHEN
        HttpGet get = new HttpGet("http://localhost:" + port + "/echoCookies");
        get.setHeader("Cookie", "Larry=Moe");

        assertResource(get, "Larry=Moe", 200);
    }

    @Test
    public void basicAuthentication(){
        // without authorization header
        assertResource(new HttpGet("http://localhost:" + port + "/secure"), "You must first log-in", 401,
                new HeaderSpec("WWW-Authenticate", "Basic realm=secure area"));

        // with authorization header
        HttpGet get = new HttpGet("http://localhost:" + port + "/secure");
        get.setHeader("Authorization", "Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ==");
        assertResource(get, "You're In!", 200);
    }

    private String printSizeInGb(BigInteger numBytes){
        final BigDecimal numBytesInOneGb = BigDecimal.valueOf(1024L * 1024L * 1024L);

        return new BigDecimal(numBytes).divide(numBytesInOneGb).toString();
    }

    @Test
    public void worksOnLargeFiles() throws Throwable{
        // given
        final Long chunkSize = 1024L * 1024L;
        final Long numChunks = 1000L; // 1 gb ... this should be coupled with a limit on the JVM size when the tests run ... will make sure things are streaming and not buffering intro ram
        final String uri = "http://localhost:" + port + "/files/" + chunkSize + "/" + numChunks;
        final BigInteger expectedNumBytesRead = BigInteger.valueOf(numChunks).multiply(BigInteger.valueOf(chunkSize));
        System.out.println("URL is " + uri);
        System.out.println("printSizeInGb is " + printSizeInGb(expectedNumBytesRead));
        final HttpGet request = new HttpGet(uri);
        final CloseableHttpClient client = HttpClients.createDefault();
        final HttpResponse response = client.execute(request);

        // when
        final BigInteger numBytesRead = streamSize(response.getEntity().getContent(), expectedNumBytesRead);

        // then
        assertEquals(200, response.getStatusLine().getStatusCode());
        assertEquals(expectedNumBytesRead, numBytesRead);

        EntityUtils.consume(response.getEntity());
        request.releaseConnection();
        client.close();
    }

    private static BigInteger streamSize(InputStream data, BigInteger expectedSize) throws IOException {
        final ProgressReporter reporter = new ProgressReporter("client read", expectedSize, null);
        BigInteger totalBytesRead = BigInteger.ZERO;

        final byte[] buffer = new byte[1024];
        boolean keepGoing = true;
        do{
            final int numRead = data.read(buffer);
            if(numRead==-1){
                keepGoing=false;
            }else{
                totalBytesRead = totalBytesRead.add(BigInteger.valueOf(numRead));
                reporter.progressMade(totalBytesRead);
            }
        }while(keepGoing);

        return totalBytesRead;
    }



    @Test
    public void nullResponsesAreTreatedAsNotFound(){
        assertResource(new HttpGet("http://localhost:" + port + "/nothing"), 404);
    }

    @Test
    public void returnsNotFoundIfThereIsNoMatchingPattern(){
        assertResource(new HttpGet("http://localhost:" + port + "/bob"), 404);
    }

    @Test
    public void happyPathForGet(){
        assertResource(new HttpGet("http://localhost:" + port + "/app"), "Welcome to the app", 200);
    }


//    @Ignore
    @Test
    public void connectionRecycling() throws IOException {
        final CloseableHttpClient client = HttpClients.createDefault();
        assertResource(client, new HttpGet("http://localhost:" + port + "/app"), "Welcome to the app", 200);
        assertResource(client, new HttpGet("http://localhost:" + port + "/app"), "Welcome to the app", 200);
        client.close();
    }


    @Test
    public void happyPathForPost(){
        assertResource(new HttpPost("http://localhost:" + port + "/app/inbox"), "Message Received", 200);
    }

    @Test
    public void happyPathForPut(){
        assertResource(withBody(new HttpPut("http://localhost:" + port + "/app/inbox/abc"), "hello world"), "hello world", 200);
    }

    @Test
    public void queryParameters(){
        assertResource(new HttpGet("http://localhost:" + port + "/echoQuery?a=1&b=2"), "a=1\nb=2", 200);
    }
    @Test
    public void queryParametersWithSlashes(){
        assertResource(new HttpGet("http://localhost:" + port + "/echoQuery?a=http://apple.com"), "a=http://apple.com", 200);
    }


    @Test
    public void urlToString(){
        assertResource(new HttpGet("http://localhost:" + port + "/echoUrl/34/marty?a=1&b=2"), "/echoUrl/34/marty?a=1&b=2", 200);
        assertResource(new HttpGet("http://localhost:" + port + "/echoUrl/44/foo"), "/echoUrl/44/foo", 200);
    }

    @Test
    public void methodNotAllowed(){
        assertResource(new HttpGet("http://localhost:" + port + "/app/inbox"), "405 Client Error: Method Not Allowed", 405);
    }

    @Test
    public void redirectsAndSetsCookies(){

        assertResource(new HttpPost("http://localhost:" + port + "/app/message"), 303,
                new HeaderSpec("Location", "/app"),
                new HeaderSpec("Set-Cookie", "name=frank"));
    }

    @Test
    public void handlesExpectContinue(){
        HttpPost request = new HttpPost("http://localhost:" + port + "/bigfiles");
        request.setHeader("Expect", "100-continue");
        byte[] data = new byte[1024 * 1024];
        request.setEntity(new ByteArrayEntity(data));
        assertResource(request, "received " + data.length + " bytes", 200);
    }


    private static <T extends HttpEntityEnclosingRequest> T withBody(T m, String body){
        try {
            m.setEntity(new StringEntity(body));
            return m;
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }


    @After
    public void tearDown() throws Exception {
        stopServing();
    }
}
