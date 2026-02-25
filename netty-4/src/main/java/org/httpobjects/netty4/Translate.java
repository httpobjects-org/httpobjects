package org.httpobjects.netty4;

import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.stream.ChunkedStream;
import org.httpobjects.*;
import org.httpobjects.header.HeaderField;
import org.httpobjects.header.request.AuthorizationField;
import org.httpobjects.header.request.RequestHeader;
import org.httpobjects.netty4.buffer.ByteAccumulator;
import org.httpobjects.path.Path;
import org.httpobjects.path.PathPattern;
import org.httpobjects.representation.InputStreamRepresentation;
import org.httpobjects.util.Method;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static io.netty.handler.codec.http.HttpHeaders.Names.*;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

public class Translate {
    public static ConnectionInfo connectionInfo(ChannelHandlerContext ctx) {
        final Channel channel = ctx.channel();
        final InetSocketAddress local = cast(channel.localAddress());
        final InetSocketAddress remote = cast(channel.remoteAddress());
        return new ConnectionInfo(
                local.getAddress().getHostAddress(),
                local.getPort(),
                remote.getAddress().getHostAddress(),
                remote.getPort());
    }


    private static <O, T extends O> T cast(O o){return (T)o;}

    public static Request readRequest(final PathPattern pathPattern, final RequestAccumulator request, final ConnectionInfo connectionInfo) {
        return readRequest(
                pathPattern,
                request.beforeBody,
                request.body,
                connectionInfo);
    }
    public static Request readRequest(final PathPattern pathPattern,
                                      final HttpRequest beforeBody,
                                      final ByteAccumulator body,
                                      final ConnectionInfo connectionInfo) {

        return new Request(){

            @Override
            public Method method() {
                return Method.fromString(beforeBody.getMethod().toString().toUpperCase());
            }

            @Override
            public boolean hasRepresentation() {
                return body!=null;
            }

            @Override
            public ConnectionInfo connectionInfo() {
                return connectionInfo;
            }

            @Override
            public RequestHeader header() {
                List<HeaderField> results = new ArrayList<HeaderField>();
                final HttpHeaders headers = beforeBody.headers();
                for(String name: headers.names()){
                    for(String value: headers.getAll(name)){
                        results.add(HeaderField.parse(name, value));
                    }
                }
                return new RequestHeader(results){
                    @Override
                    public AuthorizationField authorization() {
                        final String value = headers.get("Authorization");
                        if (value == null) return null;
                        return new AuthorizationField(value);
                    }
                };
            }

            @Override
            public Request immutableCopy() {
                return this;
            }

            @Override
            public Path path() {
                return pathPattern.match(jdkURL().getPath());
            }

            private URL jdkURL(){
                try {
                    return new URL("http://foo" + beforeBody.getUri());
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }

            }

            @Override
            public Query query() {
                return new Query(jdkURL().getQuery());
            }

            @Override
            public Representation representation() {
                String contentType = beforeBody.headers().get("Content-Type");
                InputStream data = body != null ? body.toStream() : new ByteArrayInputStream("".getBytes());
                return new InputStreamRepresentation(contentType, data);
            }
        };
    }


    public static ChannelFuture writeResponse(HttpRequest request, Channel sink, Response r, final ResponseCreationStrategy executor) {
        return writeResponse(HttpUtil.isKeepAlive(request), sink ,r, executor);
    }

    public static ChannelFuture writeResponse(boolean keepAlive, Channel sink, Response r, final ResponseCreationStrategy executor) {


        // Build the response object.
        HttpResponseStatus status = HttpResponseStatus.valueOf(r.code().value());

        DefaultHttpResponse response = new DefaultHttpResponse(HTTP_1_1, status);



        Number contentLength;
        if(r.hasRepresentation()){
            contentLength = r.representation().length();
            try{
                if(r.representation().contentType() != null){
                    response.headers().set(CONTENT_TYPE, r.representation().contentType());
                }

            }catch (Throwable t){
                throw new RuntimeException(t);
            }
        }else{
            contentLength = 0L;
        }

        if(contentLength!=null){
            response.headers().set(CONTENT_LENGTH, contentLength);
        }else{
            response.headers().set("Transfer-Encoding", "chunked");
        }


        for(HeaderField field : r.header()){
            response.headers().add(field.name(), field.value());
        }


        if (keepAlive) {
            // Add keep alive header as per:
            // - http://www.w3.org/Protocols/HTTP/1.1/draft-ietf-http-v11-spec-01.html#Connection
            response.headers().set(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
        }


        try{

            ChannelFuture headerFuture = sink.writeAndFlush(response);

            ChannelFuture writeFuture;

            if(r.hasRepresentation()){
//                System.out.println("Writing more stuff");

                ChannelPromise aggregateWritePromise = sink.newPromise();

                r.representation().data().readInputStreamAsync().then(input ->{

                    final ChunkedStream dataChunks = new ChunkedStream(input, 1024 * 1024);

                    if(contentLength!=null){
//                        System.out.println("Using normal");


                        sink.writeAndFlush(dataChunks).addListener((ChannelFutureListener) mainPartResult -> {
//                            System.out.println("Writing last content");
                            sink.writeAndFlush(new DefaultLastHttpContent())
                                 .addListener((ChannelFutureListener) lastPartResult -> {
//                                     System.out.println("totally done");
                                    aggregateWritePromise.setSuccess();
                                });
                        });
                    }else{
//                        System.out.println("Using chunked");
                        sink.writeAndFlush(new HttpChunkedInput(dataChunks))
                            .addListener((ChannelFutureListener) future -> {
                                aggregateWritePromise.setSuccess();
    //                        System.out.println("done writing response body");
                            });
                    }

                });

                writeFuture = aggregateWritePromise;
            }else{
                writeFuture = headerFuture;
            }


            // Close the non-keep-alive connection after the write operation is done.
            if (!keepAlive) {
                return writeFuture.addListener(WRITE_EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
            }else{
                return writeFuture;
            }
        }catch(Throwable t){
            throw new RuntimeException(t);
        }
    }


    private static final ChannelFutureListener WRITE_EMPTY_BUFFER = new ChannelFutureListener() {
        @Override
        public void operationComplete(ChannelFuture future) {
//            System.out.println("Closing channel");
            future.channel().writeAndFlush(Unpooled.EMPTY_BUFFER);
        }
    };

    private static byte[] read(Response out) {
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            out.representation().data().writeSync(stream);
            stream.close();
            return stream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
