package org.httpobjects.netty4;

import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpRequest;

import java.util.Date;
import java.util.UUID;

public class BasicLog implements Log {

    private final String prefix;

    public BasicLog(String prefix) {
        this.prefix = prefix;
    }

    private String base(UUID handlerId) {
        return prefix + " " + handlerId;
    }
    private String format(UUID handlerId, long requestNum) {
        return base(handlerId) + "/" + requestNum;
    }
    private void log(UUID handlerId, long requestNum, HttpRequest request, String message) {
        String requestDescription = request == null ? "null" : (request.method().name() + " " + request.uri());
        emit(format(handlerId, requestNum) + " " + requestDescription + ": " + message);
    }

    @Override
    public void requestReceived(UUID handlerId, long requestNum, HttpRequest request) {
        log( handlerId, requestNum, request, "request received");
    }

    @Override
    public void anomalyDetected(UUID handlerId, long requestNum, HttpRequest request, String message) {
        log(handlerId, requestNum, request, message);
    }

    @Override
    public void handlerCreated(UUID handlerId) {
        emit(base(handlerId) + ": handler created");
    }

    @Override
    public void contentReceivedBeforeRequest(UUID handlerId, long requestNum, HttpContent httpContent, ChannelHandlerContext ctx) {
        String contentPreview;

        if(httpContent.content().readableBytes() < (1024 * 3)){
            try {
                contentPreview = "UTF8 is " + new String(ByteBufUtil.getBytes(httpContent.content()), "UTF8");
            } catch (Throwable t) {
                contentPreview = "not text";
            }
        }else{
            contentPreview = "";
        }




        emit(format(handlerId, requestNum) + " content received before request: " + httpContent.content().readableBytes() + " bytes from " + ctx.channel().remoteAddress() + "; " + contentPreview);
    }

    @Override
    public void requestDecodeFailed(UUID traceId, UUID handlerId, long requestNum, DecoderResult decoderResult) {
        final Throwable cause = decoderResult.cause();
        emit(format(handlerId, requestNum) + " request decode failed (trace " + traceId + "): " + (cause == null ? "null" : toString(cause)));

    }

    private static String toString(Throwable t) {
        final StringBuffer text = new StringBuffer(t.getClass().getName());
        final String message = t.getMessage();
        if(message!=null){
            text.append(": ");
            text.append(message);
        }

        for(StackTraceElement next : t.getStackTrace()){
            text.append("\n    at " + next.getClassName() + "." + next.getMethodName() + "(" + next.getFileName() + ":" +  next.getLineNumber() + ")");
        }
        return text.toString();
    }


    public void emit(String formattedMessage){
        System.out.println(new Date() + " " + formattedMessage);
    }
}
