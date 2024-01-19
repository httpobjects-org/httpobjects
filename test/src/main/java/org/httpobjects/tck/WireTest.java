package org.httpobjects.tck;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.Assert;

import java.io.IOException;
import java.net.InetAddress;

public class WireTest {

    public void assertResource(HttpMethod method,
                                int expectedResponseCode, HeaderSpec ... header) {
        assertResource(method, null, expectedResponseCode, header);
    }
    public void assertResource(HttpMethod method, String expectedBody,
                                int expectedResponseCode, HeaderSpec ... header) {
        try {
            HttpClient client = new HttpClient();
            int response = client.executeMethod(method);

            Assert.assertEquals(expectedResponseCode, response);
            if(expectedBody!=null) Assert.assertEquals(expectedBody, method.getResponseBodyAsString());

            if(header!=null){
                for(HeaderSpec next : header){
                    Header h = method.getResponseHeader(next.name);
                    Assert.assertNotNull("Expected a \"" + next.name + "\" value of \"" + next.value + "\"", h);
                    Assert.assertEquals(next.value, h.getValue());
                }
            }
        } catch (HttpException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
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
            HttpClient client = new HttpClient();
            client.getHostConfiguration().setLocalAddress(InetAddress.getByName(address));
            GetMethod request = new GetMethod(url);
            int responseCode = client.executeMethod(request);
            String result = request.getResponseBodyAsString();
            return result;
        }catch(Exception e){
            throw new RuntimeException(e);
        }
    }

    public String get(String url) throws IOException, HttpException {
        HttpClient client = new HttpClient();
        GetMethod request = new GetMethod(url);
        int responseCode = client.executeMethod(request);
        String result = request.getResponseBodyAsString();
        return result;
    }
}
