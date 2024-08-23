package org.httpobjects.netty4;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.ssl.SslContext;
import io.netty.util.concurrent.Future;
import org.httpobjects.*;
import org.httpobjects.HttpObject;
import org.httpobjects.netty4.buffer.ByteAccumulatorFactory;
import org.httpobjects.netty4.buffer.InMemoryByteAccumulatorFactory;
import org.httpobjects.util.SimpleErrorHandler;

/**
 * NOTE: This class is not necessarily the right way to serve your stuff!  It's certainly not intended to cover all use cases.
 * It is, however:
 *   - a good example of how to put the pieces together in a functional server (e.g. feel free to copy+paste+edit to your liking)
 *   - a basic server implementation that may be suitable for many projects
 */
public interface BasicNetty4Server {

    static BasicNetty4Server serveHttp(int port, HttpObject ... objects) {
        return serveHttp(port, Arrays.asList(objects));
    }
    static BasicNetty4Server serveHttp(int port, List<HttpObject> objects) {
        ByteAccumulatorFactory buffers = new InMemoryByteAccumulatorFactory();
        ResponseCreationStrategy responseCreationStrategy = ResponseCreationStrategy.async(Executors.newCachedThreadPool());
        return serveHttp(port, objects, responseCreationStrategy, buffers);
    }
    static BasicNetty4Server serveHttp(int port, List<HttpObject> objects, ResponseCreationStrategy responseCreationStrategy, ByteAccumulatorFactory buffers) {
        return serve(port, objects, responseCreationStrategy, new SimpleErrorHandler(), buffers, null);
    }

    static BasicNetty4Server serveHttps(int port, SslContext sslEngine, HttpObject ... objects) {
        return serveHttps(port, Arrays.asList(objects), sslEngine);
    }
    static BasicNetty4Server serveHttps(int port, List<HttpObject> objects, SslContext sslContext) {
        ByteAccumulatorFactory buffers = new InMemoryByteAccumulatorFactory();
        ResponseCreationStrategy responseCreationStrategy = ResponseCreationStrategy.async(Executors.newCachedThreadPool());
        return serveHttps(port, objects, responseCreationStrategy, buffers, sslContext);
    }
    static BasicNetty4Server serveHttps(int port, List<HttpObject> objects, ResponseCreationStrategy responseCreationStrategy, ByteAccumulatorFactory buffers, SslContext sslContext) {
        return serve(port, objects, responseCreationStrategy, new SimpleErrorHandler(), buffers, sslContext);
    }

    static BasicNetty4Server serve(int port, List<HttpObject> objects, ResponseCreationStrategy responseCreationStrategy, ErrorHandler errorHandler, ByteAccumulatorFactory buffers, SslContext sslContext) {
        HttpObjectsResponder httpobjectsRequestHandler = new HttpObjectsResponder(objects, errorHandler);
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        Log log = new BasicLog(BasicNetty4Server.class.getSimpleName());

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
//                      .handler(new LoggingHandler(LogLevel.INFO))
                      .childHandler(new ChannelInitializer() {
                        @Override
                        protected void initChannel(Channel ch) throws Exception {
                            ChannelPipeline p = ch.pipeline();

                            if(sslContext!=null){
                                p.addLast(sslContext.newHandler(ch.alloc()));
                            }

                            p.addLast(new HttpRequestDecoder());
                            p.addLast(new HttpResponseEncoder());
                            p.addLast(new HttpobjectsChannelHandler(responseCreationStrategy, httpobjectsRequestHandler, buffers, log));
                        }
                    });

            b.bind(port).sync();

            return new BasicNetty4Server(){
                @Override
                public void shutdownGracefully(Long timeoutMillis) {
                    try{
                        final Long start = System.currentTimeMillis();
                        final Future<?> workerShutdown = workerGroup.shutdownGracefully();
                        final Future<?> bossShutdown = bossGroup.shutdownGracefully();

//                        System.out.println("Shutting down responders");
                        responseCreationStrategy.stop(timeoutMillis);

                        /**
                         * NOTE: these futures seem to take a long time, but needlessly.  So, we're just giving them a short time to complete.
                         */
//                        System.out.println("Waiting for workers");
                        workerShutdown.await(Math.min(50L, timeoutMillis - (System.currentTimeMillis() - start)));
//                        System.out.println("Waiting for boss");
                        bossShutdown.await(Math.min(50L, timeoutMillis - (System.currentTimeMillis() - start)));
//                        System.out.println("Shutdown complete");
                    }catch(Throwable t){
                        throw new RuntimeException(t);
                    }
                }
            };
        } catch(Throwable t){
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
            throw new RuntimeException("There was a problem", t);
        }
    }


    static void main(String[] args) {
        int port;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        } else {
            port = 8080;
        }

        BasicNetty4Server.serveHttp(port,
                new SyncHttpObject("/") {
                    public Response getSync(Request req) {
                        return OK(Html("<html><body>Welcome.  Click <a href=\"/yo\">here</a> for a special message.</body></html>"));
                    }
                },
                new SyncHttpObject("/yo") {
                    public Response getSync(Request req) {
                        return OK(Text("Hello world"));
                    }
                }
        );
    }

    void shutdownGracefully(Long timeoutMillis);
}