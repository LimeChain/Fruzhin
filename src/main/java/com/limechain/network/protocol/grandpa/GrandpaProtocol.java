package com.limechain.network.protocol.grandpa;

import com.limechain.network.encoding.Leb128LengthFrameDecoder;
import com.limechain.network.encoding.Leb128LengthFrameEncoder;
import io.libp2p.core.Stream;
import io.libp2p.protocol.ProtocolHandler;
import io.libp2p.protocol.ProtocolMessageHandler;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.bytes.ByteArrayEncoder;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class GrandpaProtocol extends ProtocolHandler<GrandpaController> {
    public static final int TRAFFIC_LIMIT = 1024 * 1024;
    private final GrandpaEngine engine;

    public GrandpaProtocol() {
        super(TRAFFIC_LIMIT, TRAFFIC_LIMIT);
        this.engine = new GrandpaEngine();
    }

    @NotNull
    @Override
    protected CompletableFuture<GrandpaController> onStartInitiator(Stream stream) {
        stream.pushHandler(new Leb128LengthFrameDecoder());
        stream.pushHandler(new Leb128LengthFrameEncoder());

        stream.pushHandler(new ByteArrayEncoder());
        GrandpaProtocol.NotificationHandler handler = new GrandpaProtocol.NotificationHandler(engine, stream);
        stream.pushHandler(handler);
        return CompletableFuture.completedFuture(handler);
    }

    @NotNull
    @Override
    protected CompletableFuture<GrandpaController> onStartResponder(Stream stream) {
        stream.pushHandler(new Leb128LengthFrameDecoder());
        stream.pushHandler(new Leb128LengthFrameEncoder());

        stream.pushHandler(new ByteArrayEncoder());
        GrandpaProtocol.NotificationHandler handler = new GrandpaProtocol.NotificationHandler(engine, stream);
        stream.pushHandler(handler);
        return CompletableFuture.completedFuture(handler);
    }

    static class NotificationHandler implements ProtocolMessageHandler<ByteBuf>, GrandpaController {
        private final GrandpaEngine engine;
        private final Stream stream;

        public NotificationHandler(GrandpaEngine engine, Stream stream) {
            this.engine = engine;
            this.stream = stream;
        }
        @Override
        public void onMessage(@NotNull Stream stream, ByteBuf msg) {
            byte[] messageBytes = new byte[msg.readableBytes()];
            msg.readBytes(messageBytes);
            engine.receiveRequest(messageBytes, stream.remotePeerId(), stream);
        }

        @Override
        public void sendHandshake() {
            engine.writeHandshakeToStream(stream, stream.remotePeerId());
        }

        @Override
        public void sendNeighbourMessage() {
            engine.writeNeighbourMessage(stream, stream.remotePeerId());
        }
    }
}
