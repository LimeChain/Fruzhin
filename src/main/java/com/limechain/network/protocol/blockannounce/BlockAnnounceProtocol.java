package com.limechain.network.protocol.blockannounce;

import com.limechain.network.encoding.Leb128LengthFrameDecoder;
import com.limechain.network.encoding.Leb128LengthFrameEncoder;
import com.limechain.network.protocol.blockannounce.scale.BlockAnnounceHandShake;
import com.limechain.network.protocol.blockannounce.scale.BlockAnnounceHandshakeScaleWriter;
import io.emeraldpay.polkaj.scale.ScaleCodecWriter;
import io.libp2p.core.Stream;
import io.libp2p.protocol.ProtocolHandler;
import io.libp2p.protocol.ProtocolMessageHandler;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.bytes.ByteArrayEncoder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class BlockAnnounceProtocol extends ProtocolHandler<BlockAnnounceController> {
    public static final int MAX_HANDSHAKE_SIZE = 1024 * 1024;
    public static final int MAX_NOTIFICATION_SIZE = 1024 * 1024;
    private final BlockAnnounceEngine engine;

    public BlockAnnounceProtocol() {
        super(MAX_HANDSHAKE_SIZE, MAX_NOTIFICATION_SIZE);
        this.engine = new BlockAnnounceEngine();
    }

    @Override
    protected CompletableFuture<BlockAnnounceController> onStartInitiator(Stream stream) {
        System.out.println("sending message");
        stream.pushHandler(new Leb128LengthFrameDecoder());
        stream.pushHandler(new Leb128LengthFrameEncoder());
        stream.pushHandler(new ByteArrayEncoder());

        NotificationSender handler = new NotificationSender(stream);
        stream.pushHandler(handler);
        return CompletableFuture.completedFuture(handler);
    }

    @Override
    protected CompletableFuture<BlockAnnounceController> onStartResponder(Stream stream) {
        System.out.println("received notification ?");
        NotificationHandler handler = new NotificationHandler(engine);
        stream.pushHandler(handler);
        return CompletableFuture.completedFuture(handler);
    }

    static class NotificationSender implements ProtocolMessageHandler<ByteBuf>, BlockAnnounceController {
        private final Stream stream;

        public NotificationSender(Stream stream) {
            this.stream = stream;
        }

        @Override
        public void sendHandshake(BlockAnnounceHandShake req) {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            try (ScaleCodecWriter writer = new ScaleCodecWriter(buf)) {
                writer.write(new BlockAnnounceHandshakeScaleWriter(), req);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            System.out.println("Writing handshake to stream 1");
            stream.writeAndFlush(buf.toByteArray());
        }

        @Override
        public void onMessage(Stream stream, ByteBuf msg) {
            System.out.println("Received message");
            stream.closeWrite();
        }

        @Override
        public void onClosed(Stream stream) {
            System.out.println("closed..");
        }

        @Override
        public void onException(Throwable cause) {
            System.out.println("Error");
            assert cause != null;
            System.out.println(cause.getMessage());
        }
    }

    static class NotificationHandler implements ProtocolMessageHandler<ByteBuf>, BlockAnnounceController {
        private final BlockAnnounceEngine engine;

        public NotificationHandler(BlockAnnounceEngine engine) {
            this.engine = engine;
        }

        @Override
        public void onMessage(@NotNull Stream stream, ByteBuf msg) {
            byte[] messageBytes = new byte[msg.readableBytes()];
            msg.readBytes(messageBytes);
            engine.receiveRequest(messageBytes, stream.remotePeerId(), stream);
            // Idea: use the peer id and dial it again. Maybe this will open a new stream.
        }

        @Override
        public void onClosed(@NotNull Stream stream) {
            System.out.println("Closed");
            ProtocolMessageHandler.super.onClosed(stream);
        }

        @Override
        public void onException(@Nullable Throwable cause) {
            System.out.println("Excepted: " + cause.getMessage());
            cause.printStackTrace();
            ProtocolMessageHandler.super.onException(cause);
        }

        @Override
        public void sendHandshake(BlockAnnounceHandShake req) {
            throw new IllegalStateException("Responder only!");
        }
    }
}
