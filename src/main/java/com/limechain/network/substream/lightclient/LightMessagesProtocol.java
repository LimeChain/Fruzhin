package com.limechain.network.substream.lightclient;

import com.limechain.network.substream.lightclient.pb.LightClientMessage;
import io.libp2p.core.ConnectionClosedException;
import io.libp2p.core.Stream;
import io.libp2p.protocol.ProtocolHandler;
import io.libp2p.protocol.ProtocolMessageHandler;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;

import java.util.concurrent.CompletableFuture;

public class LightMessagesProtocol extends ProtocolHandler<LightMessagesController> {
    // Sizes taken from smoldot
    public static final int MAX_REQUEST_SIZE = 1024 * 512;
    public static final int MAX_RESPONSE_SIZE = 10 * 1024 * 1024;
    private final LightMessagesEngine engine;

    public LightMessagesProtocol(LightMessagesEngine engine) {
        super(MAX_REQUEST_SIZE, MAX_RESPONSE_SIZE);
        this.engine = engine;
    }

    @Override
    protected CompletableFuture<LightMessagesController> onStartInitiator(Stream stream) {
        stream.pushHandler(new ProtobufVarint32FrameDecoder());
        stream.pushHandler(new ProtobufDecoder(LightClientMessage.Response.getDefaultInstance()));

        stream.pushHandler((new ProtobufVarint32LengthFieldPrepender()));
        stream.pushHandler((new ProtobufEncoder()));

        Sender handler = new Sender(stream);
        stream.pushHandler(handler);
        return CompletableFuture.completedFuture(handler);
    }
    
    // Class for handling outgoing requests
    static class Sender
            implements ProtocolMessageHandler<LightClientMessage.Response>,
            LightMessagesController {
        private final CompletableFuture<LightClientMessage.Response> resp = new CompletableFuture<>();
        private final Stream stream;

        public Sender(Stream stream) {
            this.stream = stream;
        }

        @Override
        public void onMessage(Stream stream, LightClientMessage.Response msg) {
            resp.complete(msg);
            stream.closeWrite();
        }

        @Override
        public CompletableFuture<LightClientMessage.Response> send(LightClientMessage.Request req) {
            stream.writeAndFlush(req);
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
