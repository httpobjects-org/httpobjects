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

import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.*;
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
        new HttpObject("/cookieSetter"){
            public Eventual<Response> get(Request req){
                return OK(
                        Text("Here are some cookies!"),
                        new SetCookieField("name", "cookie monster", "sesamestreet.com"),
                        new SetCookieField("specialGuest", "mr rogers", "mrrogers.com", "/myNeighborhood", "Wed, 13-Jan-2021 22:23:01 GMT", true),
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

    class PatchMethod extends EntityEnclosingMethod {

        public PatchMethod(String uri) {
            super(uri);
        }

        @Override
        public String getName() {
            return "PATCH";
        }
    }

    @Test
    public void supportsHead() throws Exception {
        // given
        HttpClient client = new HttpClient();
        HeadMethod request = new HeadMethod("http://localhost:" + port + "/head");

        //when
        int responseCode = client.executeMethod(request);

        // then

        assertEquals(200, responseCode);
        assertEquals("bar", request.getResponseHeader("foo").getValue());
    }

    @Test
    public void supportsOptions() throws Exception{
        // given
        HttpClient client = new HttpClient();
        OptionsMethod request = new OptionsMethod("http://localhost:" + port + "/options");

        //when
        int responseCode = client.executeMethod(request);

        // then

        assertEquals(200, responseCode);
        assertEquals("bar", request.getResponseHeader("foo").getValue());
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
        PostMethod request = new PostMethod("http://localhost:" + port + "/echoHasRepresentation");
        request.setRequestEntity(new StringRequestEntity("foo bar", "text/plain", "UTF-8"));

        // then/when
        assertResource(request, "yes", 200);
    }

    @Test
    public void immutableCopies() throws Exception {
        // given
        PostMethod request = new PostMethod("http://localhost:" + port + "/immutablecopy/no/mutation/allowed");
        request.setRequestEntity(new StringRequestEntity("foo bar", "text/plain", "UTF-8"));

        // then/when
        assertResource(request,
                "URI: /immutablecopy/no/mutation/allowed?\n" +
                "content-length=7\n" +
                "content-type=text/plain; charset=UTF-8\n" +
                "host=localhost:" + port + "\n" +
                "user-agent=Jakarta Commons-HttpClient/3.1\n" +
                "foo bar", 200);

    }

    @Test
    public void parsesPathVars() throws Exception {
        // given
        GetMethod request = new GetMethod("http://localhost:" + port + "/pows/marty/private/abc123");

        // then/when
        assertResource(request, "private marty, abc123", 200);
    }

    @Test
    public void parsesSubpaths() throws Exception {
        // given
        GetMethod request = new GetMethod("http://localhost:" + port + "/subpathEcho/i/am/my/own/grandpa");

        // then/when
        assertResource(request, "i/am/my/own/grandpa", 200);
    }

    @Test
    public void supportsPatch() throws Exception {
        // given
        PatchMethod request = new PatchMethod("http://localhost:" + port + "/patchme");
        request.setRequestEntity(new StringRequestEntity(" foo bar", "text/plain", "UTF-8"));

        // then/when
        assertResource(request, "You told me to patch! foo bar", 200);
    }

    @Test
    public void setCookieHeadersAreTranslated() throws Exception{
        // given
        GetMethod request = new GetMethod("http://localhost:" + port + "/cookieSetter");
        HttpClient client = new HttpClient();

        // when
        int response = client.executeMethod(request);

        // then
        assertEquals(200, response);
        List<Header> setCookies = sortByValue(Arrays.asList(request.getResponseHeaders("Set-Cookie")));
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
            assertEquals("Wed, 13-Jan-2021 22:23:01 GMT", cookie.expiration);
            assertEquals(Boolean.TRUE, cookie.secure);
        }
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
        GetMethod get = new GetMethod("http://localhost:" + port + "/echoCookies");
        get.setRequestHeader("Cookie", "Larry=Moe");

        assertResource(get, "Larry=Moe", 200);
    }

    @Test
    public void basicAuthentication(){
        // without authorization header
        assertResource(new GetMethod("http://localhost:" + port + "/secure"), "You must first log-in", 401,
                new HeaderSpec("WWW-Authenticate", "Basic realm=secure area"));

        // with authorization header
        GetMethod get = new GetMethod("http://localhost:" + port + "/secure");
        get.setRequestHeader("Authorization", "Basic QWxhZGRpbjpvcGVuIHNlc2FtZQ==");
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
        final GetMethod request = new GetMethod(uri);
        final HttpClient client = new HttpClient();
        final int responseCode = client.executeMethod(request);

        // when
        final BigInteger numBytesRead = streamSize(request.getResponseBodyAsStream(), expectedNumBytesRead);

        // then
        assertEquals(200, responseCode);
        assertEquals(expectedNumBytesRead, numBytesRead);
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
        assertResource(new GetMethod("http://localhost:" + port + "/nothing"), 404);
    }

    @Test
    public void returnsNotFoundIfThereIsNoMatchingPattern(){
        assertResource(new GetMethod("http://localhost:" + port + "/bob"), 404);
    }

    @Test
    public void happyPathForGet(){
        assertResource(new GetMethod("http://localhost:" + port + "/app"), "Welcome to the app", 200);
    }


//    @Ignore
    @Test
    public void connectionRecycling(){
        final HttpClient client = new HttpClient();
        assertResource(client, new GetMethod("http://localhost:" + port + "/app"), "Welcome to the app", 200);
        assertResource(client, new GetMethod("http://localhost:" + port + "/app"), "Welcome to the app", 200);
    }


    @Test
    public void happyPathForPost(){
        assertResource(new PostMethod("http://localhost:" + port + "/app/inbox"), "Message Received", 200);
    }

    @Test
    public void happyPathForPut(){
        assertResource(withBody(new PutMethod("http://localhost:" + port + "/app/inbox/abc"), "hello world"), "hello world", 200);
    }

    @Test
    public void queryParameters(){
        assertResource(new GetMethod("http://localhost:" + port + "/echoQuery?a=1&b=2"), "a=1\nb=2", 200);
    }
    @Test
    public void queryParametersWithSlashes(){
        assertResource(new GetMethod("http://localhost:" + port + "/echoQuery?a=http://apple.com"), "a=http://apple.com", 200);
    }


    @Test
    public void urlToString(){
        assertResource(new GetMethod("http://localhost:" + port + "/echoUrl/34/marty?a=1&b=2"), "/echoUrl/34/marty?a=1&b=2", 200);
        assertResource(new GetMethod("http://localhost:" + port + "/echoUrl/44/foo"), "/echoUrl/44/foo", 200);
    }

    @Test
    public void methodNotAllowed(){
        assertResource(new GetMethod("http://localhost:" + port + "/app/inbox"), "405 Client Error: Method Not Allowed", 405);
    }

    @Test
    public void redirectsAndSetsCookies(){

        assertResource(new PostMethod("http://localhost:" + port + "/app/message"), 303,
                new HeaderSpec("Location", "/app"),
                new HeaderSpec("Set-Cookie", "name=frank"));
    }

    @Test
    public void handlesExpectContinue(){
        PostMethod request = new PostMethod("http://localhost:" + port + "/bigfiles");
        request.setUseExpectHeader(true);
        byte[] data = new byte[1024 * 1024];
        request.setRequestEntity(new ByteArrayRequestEntity(data));
        assertResource(request, "received " + data.length + " bytes", 200);
    }


    @SuppressWarnings("deprecation")
    private static <T extends EntityEnclosingMethod> T withBody(T m, String body){
        m.setRequestBody(body);
        return m;
    }


    @After
    public void tearDown() throws Exception {
        stopServing();
    }
}
