package org.httpobjects.clientjdk;

import org.httpobjects.client.HttpClient;
import org.httpobjects.test.client.ClientTests;

public class JdkClientTest extends ClientTests {

    @Override
    public HttpClient makeTestSubject(){
        return new JdkClient();
    }

}
