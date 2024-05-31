package com.limechain.network.protocol.state;

import com.limechain.network.protocol.sync.pb.SyncMessage;
import io.libp2p.core.Stream;
import io.libp2p.protocol.ProtocolHandler;
import io.libp2p.protocol.ProtocolMessageHandler;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingDeque;

public class StateProtocol extends ProtocolHandler<StateController> {
    public static final int MAX_REQUEST_SIZE = 1024 * 512;
    public static final int MAX_RESPONSE_SIZE = 5 * 1024 * 1024 * 1024; //1gb

    public StateProtocol() {
        super(MAX_REQUEST_SIZE, MAX_RESPONSE_SIZE);
    }

    @Override
    protected CompletableFuture<StateController> onStartInitiator(Stream stream) {
        stream.pushHandler(new ProtobufVarint32FrameDecoder());
        stream.pushHandler(new ProtobufDecoder(SyncMessage.StateResponse.getDefaultInstance()));

        stream.pushHandler((new ProtobufVarint32LengthFieldPrepender()));
        stream.pushHandler((new ProtobufEncoder()));

        Sender handler = new Sender(stream);
        stream.pushHandler(handler);
        return CompletableFuture.completedFuture(handler);
    }

    static class Sender implements ProtocolMessageHandler<SyncMessage.StateResponse>, StateController {
        public static final int MAX_QUEUE_SIZE = 50;
        private final LinkedBlockingDeque<CompletableFuture<SyncMessage.StateResponse>> queue =
                new LinkedBlockingDeque<>(MAX_QUEUE_SIZE);

        private final Stream stream;

        public Sender(Stream stream) {
            this.stream = stream;
        }

        @Override
        public void onMessage(Stream stream, SyncMessage.StateResponse msg) {
            if (msg.getEntriesList().stream().anyMatch(SyncMessage.KeyValueStateEntry::getComplete)) {
                Objects.requireNonNull(queue.poll()).complete(msg);
                stream.closeWrite();
            } else {
                Objects.requireNonNull(queue.poll()).complete(msg);
            }
        }

        @Override
        public CompletableFuture<SyncMessage.StateResponse> send(SyncMessage.StateRequest req) {
            CompletableFuture<SyncMessage.StateResponse> res = new CompletableFuture<>();
            queue.add(res);
            stream.writeAndFlush(req);
            return res;
        }

        @Override
        public void onException(Throwable cause) {
            Objects.requireNonNull(queue.poll()).completeExceptionally(cause);
        }

    }
}
