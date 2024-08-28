package org.httpobjects.client;

import org.httpobjects.test.client.ClientTests;

public class ApacheCommons4xHttpClientTest extends ClientTests {
    @Override
    public HttpClient makeTestSubject() {
        return new ApacheCommons4xHttpClient();
    }
}
