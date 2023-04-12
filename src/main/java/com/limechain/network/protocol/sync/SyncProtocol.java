package com.limechain.network.protocol.sync;

import com.limechain.network.substream.sync.pb.SyncMessage;
import io.libp2p.core.ConnectionClosedException;
import io.libp2p.core.Stream;
import io.libp2p.protocol.ProtocolHandler;
import io.libp2p.protocol.ProtocolMessageHandler;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;

import java.util.concurrent.CompletableFuture;

public class SyncProtocol extends ProtocolHandler<SyncController> {
    public static final int MAX_REQUEST_SIZE = 1024 * 512;
    public static final int MAX_RESPONSE_SIZE = 10 * 1024 * 1024;

    public SyncProtocol() {
        super(MAX_REQUEST_SIZE, MAX_RESPONSE_SIZE);
    }

    @Override
    protected CompletableFuture<SyncController> onStartInitiator(Stream stream) {
        stream.pushHandler(new ProtobufVarint32FrameDecoder());
        stream.pushHandler(new ProtobufDecoder(SyncMessage.BlockResponse.getDefaultInstance()));

        stream.pushHandler((new ProtobufVarint32LengthFieldPrepender()));
        stream.pushHandler((new ProtobufEncoder()));

        Sender handler = new Sender(stream);
        stream.pushHandler(handler);
        return CompletableFuture.completedFuture(handler);
    }

    // Class for handling outgoing requests
    static class Sender
            implements ProtocolMessageHandler<SyncMessage.BlockResponse>,
            SyncController {
        private final CompletableFuture<SyncMessage.BlockResponse> resp = new CompletableFuture<>();
        private final Stream stream;

        public Sender(Stream stream) {
            this.stream = stream;
        }

        @Override
        public void onMessage(Stream stream, SyncMessage.BlockResponse msg) {
            resp.complete(msg);
            stream.closeWrite();
        }

        @Override
        public CompletableFuture<SyncMessage.BlockResponse> send(SyncMessage.BlockRequest msg) {
            stream.writeAndFlush(msg);
            return resp;
        }

        @Override
        public void onClosed(Stream stream) {
            resp.completeExceptionally(new ConnectionClosedException());
        }

        @Override
        public void onException(Throwable cause) {
            resp.completeExceptionally(cause);
        }

    }
}
