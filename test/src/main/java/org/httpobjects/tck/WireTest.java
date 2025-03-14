package org.httpobjects.tck;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.Assert;

import java.io.IOException;
import java.net.InetAddress;

public class WireTest {

    public void assertResource(HttpRequestBase request,
                                int expectedResponseCode, HeaderSpec ... header) {
        assertResource(request, null, expectedResponseCode, header);
    }
    public void assertResource(HttpRequestBase request, String expectedBody,
                                int expectedResponseCode, HeaderSpec ... header) {

        CloseableHttpClient client = HttpClients.createDefault();
        assertResource(HttpClients.createDefault(), request, expectedBody, expectedResponseCode, header);

        try {
            client.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void assertResource(HttpClient client, HttpRequestBase request, String expectedBody,
                               int expectedResponseCode, HeaderSpec ... header) {
        try {
            // httpclient 4.x follows redirects by default.  Don't follow redirects in order to not break redirectsAndSetsCookies test.
            RequestConfig requestConfig = RequestConfig.custom().setRedirectsEnabled(false).build();
            request.setConfig(requestConfig);

            System.out.println(request.getMethod() + " " + request.getURI());
            HttpResponse response = client.execute(request);
            int statusCode = response.getStatusLine().getStatusCode();
            System.out.println(request.getMethod() + " " + request.getURI() + ": " + statusCode);

            Assert.assertEquals(expectedResponseCode, statusCode);

            if(expectedBody!=null) {
                final String actualBody = EntityUtils.toString(response.getEntity());
                System.out.println("got: " + actualBody);
                Assert.assertEquals(expectedBody, actualBody);
            }

            if(header!=null){
                for(HeaderSpec next : header){
                    Header h = response.getFirstHeader(next.name);
                    Assert.assertNotNull("Expected a \"" + next.name + "\" value of \"" + next.value + "\"", h);
                    Assert.assertEquals(next.value, h.getValue());
                }
            }

            EntityUtils.consume(response.getEntity());
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            request.releaseConnection();
        }
    }

    public class HeaderSpec {
        final String name;
        final String value;
        HeaderSpec(String name, String value) {
            super();
            this.name = name;
            this.value = value;
        }

    }

    public String getFrom(String address, String url) {
        try {
            InetAddress localAddress = InetAddress.getByName(address);
            RequestConfig config = RequestConfig.custom()
                    .setLocalAddress(localAddress)
                    .build();

            CloseableHttpClient client = HttpClients.custom()
                    .setDefaultRequestConfig(config)
                    .build();

            HttpGet request = new HttpGet(url);
            HttpResponse response = client.execute(request);
            String result = EntityUtils.toString(response.getEntity());

            EntityUtils.consume(response.getEntity());
            request.releaseConnection();
            client.close();

            return result;
        }catch(Exception e){
            throw new RuntimeException(e);
        }
    }

    public String get(String url) throws IOException {
        CloseableHttpClient client = HttpClients.createDefault();
        HttpGet request = new HttpGet(url);
        HttpResponse response = client.execute(request);
        String result = EntityUtils.toString(response.getEntity());

        EntityUtils.consume(response.getEntity());
        request.releaseConnection();
        client.close();

        return result;
    }
}
