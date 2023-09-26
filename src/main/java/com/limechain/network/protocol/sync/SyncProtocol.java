package com.limechain.network.protocol.sync;

import com.google.protobuf.InvalidProtocolBufferException;
import com.limechain.network.protocol.sync.pb.SyncMessage;
import io.libp2p.core.Stream;
import io.libp2p.protocol.ProtocolHandler;
import io.libp2p.protocol.ProtocolMessageHandler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import lombok.extern.java.Log;
import org.apache.tomcat.util.buf.HexUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingDeque;

public class SyncProtocol extends ProtocolHandler<SyncController> {
    public static final int MAX_REQUEST_SIZE = 1024 * 512;
    public static final int MAX_RESPONSE_SIZE = 10 * 1024 * 1024;

    public SyncProtocol() {
        super(MAX_REQUEST_SIZE, MAX_RESPONSE_SIZE);
    }

    @NotNull
    private static <T extends ProtocolMessageHandler<ByteBuf> & SyncController> CompletableFuture<SyncController>
    onStartStream(Stream stream, T handler) {
        stream.pushHandler(new ProtobufVarint32FrameDecoder());

        stream.pushHandler((new ProtobufVarint32LengthFieldPrepender()));
        stream.pushHandler((new ProtobufEncoder()));

        stream.pushHandler(handler);
        return CompletableFuture.completedFuture(handler);
    }

    @Override
    protected CompletableFuture<SyncController> onStartInitiator(@NotNull Stream stream) {
        return onStartStream(stream, new Sender(stream));
    }

    @Override
    protected CompletableFuture<SyncController> onStartResponder(@NotNull Stream stream) {
        return onStartStream(stream, new Receiver(stream));
    }

    // Class for handling outgoing requests
    @Log
    static class Sender
            implements ProtocolMessageHandler<ByteBuf>,
            SyncController {
        public static final int MAX_QUEUE_SIZE = 50;
        private final LinkedBlockingDeque<CompletableFuture<byte[]>> queue =
                new LinkedBlockingDeque<>(MAX_QUEUE_SIZE);

        private final Stream stream;

        public Sender(Stream stream) {
            this.stream = stream;
        }

        @Override
        public void onMessage(@NotNull Stream stream, ByteBuf msg) {
            int length = msg.readableBytes();
            byte[] array;
            if (msg.hasArray()) {
                array = msg.array();
            } else {
                array = ByteBufUtil.getBytes(msg, msg.readerIndex(), length, false);
            }

            Objects.requireNonNull(queue.poll()).complete(array);
            stream.closeWrite();
        }

        @Override
        public <T, E> CompletableFuture<E> send(T req, Class<E> resClass) {
            CompletableFuture<byte[]> res = new CompletableFuture<>();
            queue.add(res);
            stream.writeAndFlush(req);
            return res.thenApply(msg -> parseResponse(msg, resClass));
        }

        private <E> E parseResponse(byte[] msg, Class<E> resClass) {
            try {
                if (SyncMessage.BlockResponse.class.equals(resClass)) {
                    return (E) SyncMessage.BlockResponse.parseFrom(msg);
                } else if (SyncMessage.StateResponse.class.equals(resClass)) {
                    return (E) SyncMessage.StateResponse.parseFrom(msg);
                }
            } catch (InvalidProtocolBufferException e) {
                log.severe("Error while parsing response: " + e.getMessage());
                return null;
            }
            log.severe("Unknown response class: " + resClass);
            return null;
        }

        @Override
        public void onException(Throwable cause) {
            Objects.requireNonNull(queue.poll()).completeExceptionally(cause);
        }

    }

    @Log
    static class Receiver implements ProtocolMessageHandler<ByteBuf>, SyncController {
        private final Stream stream;

        public Receiver(Stream stream) {
            this.stream = stream;
        }

        @Override
        public void onMessage(@NotNull Stream stream, ByteBuf msg) {
            int length = msg.readableBytes();
            byte[] array;
            if (msg.hasArray()) {
                array = msg.array();
            } else {
                array = ByteBufUtil.getBytes(msg, msg.readerIndex(), length, false);
            }

            SyncMessage.BlockRequest blockRequest = null;
            SyncMessage.StateRequest stateRequest = null;
            try {
                blockRequest = SyncMessage.BlockRequest.parseFrom(array);
            } catch (InvalidProtocolBufferException e) {
                //Empty catch block
            }
            try {
                stateRequest = SyncMessage.StateRequest.parseFrom(array);
            } catch (InvalidProtocolBufferException e) {
                //Empty catch block
            }

            if (blockRequest != null) {
                handleBlockRequest(blockRequest);
            } else if (stateRequest != null) {
                handleStateRequest(stateRequest);
            } else {
                log.severe("Received unknown request with data " + HexUtils.toHexString(array));
            }
        }

        private void handleStateRequest(SyncMessage.StateRequest stateRequest) {
            //Todo: Create state response
//            SyncMessage.StateResponse stateResponse = SyncMessage.StateResponse.newBuilder().build();
//            stream.writeAndFlush(stateResponse);
            stream.closeWrite();
        }

        private void handleBlockRequest(SyncMessage.BlockRequest blockRequest) {
            //Todo: create block response (must get the block from the storage)
//            SyncMessage.BlockResponse blockResponse = SyncMessage.BlockResponse.newBuilder().build();
//            stream.writeAndFlush(blockResponse);
            stream.closeWrite();
        }
    }
}
