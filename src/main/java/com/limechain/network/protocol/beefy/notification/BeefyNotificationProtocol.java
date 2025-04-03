package com.limechain.network.protocol.beefy.notification;

import com.limechain.network.ConnectionManager;
import com.limechain.network.protocol.base.BaseProtocol;
import io.libp2p.core.Stream;
import io.libp2p.protocol.ProtocolMessageHandler;
import io.netty.buffer.ByteBuf;
import lombok.extern.java.Log;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;

/**
 * Handler for BEEFY notification protocol messages and streams
 */
@Log
public class BeefyNotificationProtocol extends BaseProtocol<BeefyNotificationController, BeefyNotificationProtocol.NotificationHandler> {

    private static final long TRAFFIC_LIMIT = Long.MAX_VALUE;

    /**
     * Creates a handler with {@link Long#MAX_VALUE} traffic limit.
     * This is a global decreasing limit for the protocol, that gets reduced by the size of each message.
     * In the future it should be changed to a per-message limit
     */
    public BeefyNotificationProtocol() {
        super(TRAFFIC_LIMIT, TRAFFIC_LIMIT);
    }

    @Override
    protected BeefyNotificationProtocol.NotificationHandler createNotificationHandler(Stream stream) {
        return new BeefyNotificationProtocol.NotificationHandler(stream);
    }

    /**
     * Handler for notifications received on the BEEFY protocol
     */
    static class NotificationHandler extends BeefyNotificationController implements ProtocolMessageHandler<ByteBuf> {

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
        public void onClosed(Stream stream) {
            connectionManager.closeBeefyStream(stream);
            log.log(Level.INFO, "Beefy stream closed for peer " + stream.remotePeerId());
            ProtocolMessageHandler.super.onClosed(stream);
        }

        @Override
        public void onException(Throwable cause) {
            connectionManager.closeBeefyStream(stream);
            if (cause != null) {
                log.log(Level.WARNING, "Beefy Exception: " + cause.getMessage());
            } else {
                log.log(Level.WARNING, "Beefy Exception with unknown cause");
            }
            ProtocolMessageHandler.super.onException(cause);
        }
    }
}
