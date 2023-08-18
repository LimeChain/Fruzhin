package com.limechain.network.protocol.blockannounce;

import com.limechain.network.encoding.Leb128LengthFrameDecoder;
import com.limechain.network.encoding.Leb128LengthFrameEncoder;
import io.libp2p.core.Stream;
import io.libp2p.protocol.ProtocolHandler;
import io.libp2p.protocol.ProtocolMessageHandler;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.bytes.ByteArrayEncoder;
import lombok.extern.java.Log;
import java.util.logging.Level;

import java.util.concurrent.CompletableFuture;

@Log
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
        stream.pushHandler(new Leb128LengthFrameDecoder());
        stream.pushHandler(new Leb128LengthFrameEncoder());

        stream.pushHandler(new ByteArrayEncoder());
        NotificationHandler handler = new NotificationHandler(engine, stream);
        stream.pushHandler(handler);
        return CompletableFuture.completedFuture(handler);
    }

    @Override
    protected CompletableFuture<BlockAnnounceController> onStartResponder(Stream stream) {
        stream.pushHandler(new Leb128LengthFrameDecoder());
        stream.pushHandler(new Leb128LengthFrameEncoder());

        stream.pushHandler(new ByteArrayEncoder());
        NotificationHandler handler = new NotificationHandler(engine, stream);
        stream.pushHandler(handler);
        return CompletableFuture.completedFuture(handler);
    }

    static class NotificationHandler implements ProtocolMessageHandler<ByteBuf>, BlockAnnounceController {
        private final BlockAnnounceEngine engine;
        private final Stream stream;

        public NotificationHandler(BlockAnnounceEngine engine, Stream stream) {
            this.engine = engine;
            this.stream = stream;
        }

        @Override
        public void onMessage(Stream stream, ByteBuf msg) {
            byte[] messageBytes = new byte[msg.readableBytes()];
            msg.readBytes(messageBytes);
            engine.receiveRequest(messageBytes, stream.remotePeerId(), stream);
        }

        @Override
        public void onClosed(Stream stream) {
            log.log(Level.INFO, "Block announce stream closed for peer " + stream.remotePeerId());
            engine.removePeerHandshake(stream.remotePeerId());
            ProtocolMessageHandler.super.onClosed(stream);
        }

        @Override
        public void onException(Throwable cause) {
            log.log(Level.WARNING, "Block Announce Exception: " + cause.getMessage());
            cause.printStackTrace();
            ProtocolMessageHandler.super.onException(cause);
        }

        @Override
        public void sendHandshake() {
            engine.writeHandshakeToStream(stream, stream.remotePeerId());
        }
    }
}
