package org.httpobjects.netty4;

import static io.netty.handler.codec.http.HttpResponseStatus.CONTINUE;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import io.netty.channel.*;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.*;
import org.httpobjects.DSL;
import org.httpobjects.Response;
import org.httpobjects.eventual.EventualResult;
import org.httpobjects.netty4.buffer.ByteAccumulatorFactory;

import java.util.*;

public class HttpobjectsChannelHandler extends SimpleChannelInboundHandler<Object> {
    final UUID id = UUID.randomUUID();
    private final ByteAccumulatorFactory bufferFactory;
    private final HttpObjectsResponder responder;
    private final ResponseCreationStrategy responseCreator;
    private final Log log;

    private long requestCounter = 0;
    private RequestAccumulator currentRequest;

    public HttpobjectsChannelHandler(ResponseCreationStrategy responseCreator, HttpObjectsResponder responder, ByteAccumulatorFactory bufferFactory, Log log) {
        this.responseCreator = responseCreator;
        this.bufferFactory = bufferFactory;
        this.responder = responder;
        this.log = log;
        log.handlerCreated(id);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) {

        if (msg instanceof HttpRequest) {
            HttpRequest request = (HttpRequest) msg;

            requestCounter ++;
            if(request.decoderResult().isFailure()){
                final UUID traceId = UUID.randomUUID();
                log.requestDecodeFailed(traceId, id, requestCounter, request.decoderResult());
                this.currentRequest = null;
                Translate.writeResponse(
                        false,
                        ctx.channel(),
                        DSL.BAD_REQUEST(DSL.Text("Trace " + traceId)));
            }else{
                this.currentRequest = new RequestAccumulator(request, bufferFactory, log, id, requestCounter);

                if (HttpUtil.is100ContinueExpected(request)) {
                    ctx.write(new DefaultFullHttpResponse(HTTP_1_1, CONTINUE, Unpooled.EMPTY_BUFFER));
                }
            }

        }
        if (msg instanceof HttpContent) {

            HttpContent httpContent = (HttpContent) msg;
            ByteBuf content = httpContent.content();

            if(this.currentRequest==null){
                log.contentReceivedBeforeRequest(id, requestCounter, httpContent, ctx);
            }else{
                this.currentRequest.appendBody(content);

                if (msg instanceof LastHttpContent) {

                    responseCreator.doIt(new Runnable() {
                        @Override
                        public void run() {
                            responder.respond(currentRequest, Translate.connectionInfo(ctx)).then(new EventualResult.ResultHandler<Response>() {
                                @Override
                                public void exec(Response response) {

                                    Translate.writeResponse(
                                            currentRequest.beforeBody,
                                            ctx.channel(),
                                            response);
                                    currentRequest.dispose();
                                }
                            });
                        }
                    });
                }
            }

        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
