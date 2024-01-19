package org.httpobjects.netty4;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpRequest;

import java.util.UUID;

public interface Log {
    void requestReceived(UUID handlerId, long requestNum, HttpRequest request);
    void anomalyDetected(UUID handlerId, long requestNum, HttpRequest request, String message);
    void handlerCreated(UUID handlerId);
    void contentReceivedBeforeRequest(UUID handlerId, long requestNum, HttpContent httpContent, ChannelHandlerContext ctx);
    void requestDecodeFailed(UUID traceId, UUID handlerId, long requestNum, DecoderResult decoderResult);
}