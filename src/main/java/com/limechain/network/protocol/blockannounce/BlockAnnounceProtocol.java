package com.limechain.network.protocol.blockannounce;

import com.limechain.network.ConnectionManager;
import com.limechain.network.protocol.base.BaseProtocol;
import io.libp2p.core.Stream;
import io.libp2p.protocol.ProtocolMessageHandler;
import io.netty.buffer.ByteBuf;
import lombok.extern.java.Log;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;

/**
 * Handler for BlockAnnounce protocol messages and streams
 */
@Log
public class BlockAnnounceProtocol extends BaseProtocol<BlockAnnounceController, BlockAnnounceProtocol.NotificationHandler> {

    public static final long MAX_HANDSHAKE_SIZE = 1024L * 1024L;
    public static final long MAX_NOTIFICATION_SIZE = 1024L * 1024L;

    public BlockAnnounceProtocol() {
        super(MAX_HANDSHAKE_SIZE, MAX_NOTIFICATION_SIZE);
    }

    @Override
    protected BlockAnnounceProtocol.NotificationHandler createNotificationHandler(Stream stream) {
        return new BlockAnnounceProtocol.NotificationHandler(stream);
    }

    static class NotificationHandler extends BlockAnnounceController implements ProtocolMessageHandler<ByteBuf> {
        ConnectionManager connectionManager = ConnectionManager.getInstance();
        public NotificationHandler(Stream stream) {
            super(stream);
        }

        @Override
        public void onMessage(@NotNull Stream stream, ByteBuf msg) {
            byte[] messageBytes = new byte[msg.readableBytes()];
            msg.readBytes(messageBytes);
            engine.receiveRequest(messageBytes, stream);
        }

        @Override
        public void onClosed(@NotNull Stream stream) {
            connectionManager.closeBlockAnnounceStream(stream);
            log.log(Level.INFO, "Block announce stream closed for peer " + stream.remotePeerId());
            ProtocolMessageHandler.super.onClosed(stream);
        }

        @Override
        public void onException(Throwable cause) {
            connectionManager.closeBlockAnnounceStream(stream);
            if (cause != null) {
                log.log(Level.WARNING, "Block Announce Exception: " + cause.getMessage());
            } else {
                log.log(Level.WARNING, "Block Announce Exception with unknown cause");
            }
            ProtocolMessageHandler.super.onException(cause);
        }
    }
}
