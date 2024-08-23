package org.httpobjects.netty;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.httpobjects.HttpObject;
import org.httpobjects.Request;
import org.httpobjects.Response;
import org.httpobjects.SyncHttpObject;
import org.httpobjects.netty.http.ByteAccumulatorFactory;
import org.httpobjects.netty.http.HttpServerPipelineFactory;
import org.httpobjects.netty.http.InMemoryByteAccumulatorFactory;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;


public class HttpobjectsNettySupport {
      public static class ServerWrapper {
          public final Integer port;
          private final Channel channel;
          private final ExecutorService bossExecutor;
          private final ExecutorService workExecutor;

          public ServerWrapper(Integer port, Channel channel, ExecutorService bossExecutor, ExecutorService workExecutor) {
              this.port = port;
              this.channel = channel;
              this.bossExecutor = bossExecutor;
              this.workExecutor = workExecutor;
          }

          public void close(){
              this.stop();
          }
          public void stop() {
              try {
                  bossExecutor.shutdownNow();
                  workExecutor.shutdownNow();
//                  bossExecutor.awaitTermination(5000L, TimeUnit.MILLISECONDS);
//                  workExecutor.awaitTermination(5000L, TimeUnit.MILLISECONDS);
                  channel.unbind().sync();
              }catch (Throwable t){
                  throw new RuntimeException(t);
              }
          }
      }

      public static ServerWrapper serve(int port, HttpObject ... objects) {
		return serve(port, Arrays.asList(objects));
      }
      public static ServerWrapper serve(int port, List<HttpObject> objects) {
          ByteAccumulatorFactory buffers = new InMemoryByteAccumulatorFactory();
          return serve(port, objects, buffers);
      }

      public static ServerWrapper serve(int port, List<HttpObject> objects, ByteAccumulatorFactory buffers) {
          // Configure the server.
          ExecutorService bossExecutor = Executors.newCachedThreadPool();
          ExecutorService workExecutor = Executors.newCachedThreadPool();
          ServerBootstrap bootstrap = new ServerBootstrap(
                  new NioServerSocketChannelFactory(
                          bossExecutor,
                          workExecutor));
  
          // Set up the event pipeline factory.
          bootstrap.setPipelineFactory(new HttpServerPipelineFactory(new NettyHttpobjectsRequestHandler(objects), buffers));
  
          // Bind and start to accept incoming connections.
          return new ServerWrapper(port, bootstrap.bind(new InetSocketAddress(port)), bossExecutor, workExecutor) ;
      }
  
      public static void main(String[] args) {
          int port;
          if (args.length > 0) {
              port = Integer.parseInt(args[0]);
          } else {
              port = 8080;
          }
          HttpobjectsNettySupport.serve(port, Arrays.<HttpObject>asList(
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
              		));
      }
}
