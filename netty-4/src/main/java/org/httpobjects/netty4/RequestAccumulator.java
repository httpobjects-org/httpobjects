package org.httpobjects.netty4;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpRequest;
import org.httpobjects.netty4.buffer.ByteAccumulator;
import org.httpobjects.netty4.buffer.ByteAccumulatorFactory;

import java.util.UUID;

public class RequestAccumulator {
    final HttpRequest beforeBody;
    final ByteAccumulator body;
    private final Log log;
    final UUID handlerId;
    final long requestId;
    private boolean hasDisposed = false;

    RequestAccumulator(HttpRequest request, ByteAccumulatorFactory contentAccumulators, Log log, UUID handlerId, long requestId) {
        this.beforeBody = request;
        this.body = contentAccumulators.newAccumulator();
        this.log = log;
        this.handlerId = handlerId;
        this.requestId = requestId;
        log.requestReceived(handlerId, requestId, beforeBody);
    }

    void appendBody(ByteBuf content) {
        if(hasDisposed) {
            log.anomalyDetected(handlerId, requestId, beforeBody, " ignoring " + content.capacity() + " content bytes received after disposal");
        }else{
            try {
                content.getBytes(0, body.out(), content.capacity());
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }
    }

    public void dispose() {
        hasDisposed = true;
        body.dispose();
    }
}