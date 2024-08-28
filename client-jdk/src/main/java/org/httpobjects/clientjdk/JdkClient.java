package org.httpobjects.clientjdk;

import org.httpobjects.Representation;
import org.httpobjects.Response;
import org.httpobjects.ResponseCode;
import org.httpobjects.client.HttpClient;
import org.httpobjects.data.DataSource;
import org.httpobjects.data.OutputStreamDataSource;
import org.httpobjects.header.HeaderField;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.stream.Collectors;

public class JdkClient implements HttpClient {
    private final java.net.http.HttpClient client;

    public JdkClient(){
        this(java.net.http.HttpClient.newBuilder().build());
    }

    public JdkClient(java.net.http.HttpClient client) {
        this.client = client;
    }

    @Override
    public RemoteObject resource(String uri) {
        return new JdkRemoteObject(uri, client);
    }
}

class JdkRemoteObject extends HttpClient.RemoteObject {
    private final String uri;
    private final java.net.http.HttpClient client;

    JdkRemoteObject(String uri, java.net.http.HttpClient client) {
        this.uri = uri;
        this.client = client;
    }

    @Override
    public Response get(Representation r, String query, HeaderField... fields) {
        return doIt("GET",  r, query, fields);
    }

    @Override
    public Response post(Representation r, String query, HeaderField... fields) {

        return doIt("POST",   r, query, fields);
    }
    @Override
    public Response put(Representation r, String query, HeaderField... fields) {
        return doIt("PUT",   r, query, fields);
    }

    @Override
    public Response delete(Representation r, String query, HeaderField... fields) {
        return doIt("DELETE",   r, query, fields);
    }

    @Override
    public Response patch(Representation r, String query, HeaderField... fields) {
        return doIt("PATCH",  r, query, fields);
    }

    @Override
    public Response head(Representation r, String query, HeaderField... fields) {
        return doIt("HEAD",   r, query, fields);
    }

    @Override
    public Response options(Representation r, String query, HeaderField... fields) {
        return doIt("OPTIONS",  r, query, fields);
    }

    @Override
    public Response trace(Representation r, String query, HeaderField... fields) {
        return doIt("TRACE",  r, query, fields);
    }

    private Response doIt(String method, Representation r, String query, HeaderField[] fields) {
        System.out.println("[" + method + "] " + uri);
        try{
            final HttpRequest.BodyPublisher body = r == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofInputStream(() -> r.data().readInputStreamAsync().join());
            final HttpRequest request =  withHeaders(HttpRequest.newBuilder(), fields)
                    .uri(URI.create(this.uri + query))
                    .method(method, body)
                    .build();
            final HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

            return translateResponse(response);
        }catch(Throwable t){
            throw new RuntimeException(t);
        }
    }

    private HttpRequest.Builder withHeaders(HttpRequest.Builder builder, HeaderField... fields){
        for(HeaderField field : fields){
            builder.header(field.name(), field.value());
        }

        return builder;
    }

    private Response translateResponse(HttpResponse<byte[]> response) {
        return new Response(
                ResponseCode.forCode(response.statusCode()),
                getRepresentation(response),
                getHeaderFields(response));
    }

    private static HeaderField[] getHeaderFields(HttpResponse<byte[]> response) {
        return response.headers().map().entrySet().stream().flatMap(entry->{
            return entry.getValue().stream().map(value->HeaderField.parse(entry.getKey(), value));
        }).collect(Collectors.toList()).toArray(new HeaderField[]{});
    }

    private static Representation getRepresentation(HttpResponse<byte[]> response) {
        final byte[] body = response.body();
        final Representation representation;
        if(body ==null){
            representation = null;
        }else{
            representation = new Representation() {
                @Override
                public String contentType() {
                    return response.headers().firstValue("Content-Type").orElseGet(()->null);
                }

                @Override
                public DataSource data() {
                    return new OutputStreamDataSource(out->{
                        try{
                            out.write(body);
                        }catch(Throwable t){
                            throw new RuntimeException(t);
                        }
                    });
                }
            };
        }
        return representation;
    }


}