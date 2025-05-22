package com.limechain.network.protocol.transaction;

import com.limechain.network.ConnectionManager;
import com.limechain.network.protocol.base.BaseProtocol;
import io.libp2p.core.Stream;
import io.libp2p.protocol.ProtocolMessageHandler;
import io.netty.buffer.ByteBuf;
import lombok.extern.java.Log;
import org.jetbrains.annotations.NotNull;

/**
 * Handler for Transactions protocol messages and streams.
 */
@Log
public class TransactionProtocol extends BaseProtocol<TransactionController, TransactionProtocol.NotificationHandler> {

    private static final long TRAFFIC_LIMIT = Long.MAX_VALUE;

    /**
     * Creates a handler with {@link Long#MAX_VALUE} traffic limit.
     * This is a global decreasing limit for the protocol, that gets reduced by the size of each message.
     * In the future it should be changed to a per-message limit
     */
    public TransactionProtocol() {
        super(TRAFFIC_LIMIT, TRAFFIC_LIMIT);
    }

    @Override
    protected TransactionProtocol.NotificationHandler createNotificationHandler(Stream stream) {
        return new TransactionProtocol.NotificationHandler(stream);
    }

    /**
     * Handler for notifications received on the Transactions protocol.
     */
    static class NotificationHandler extends TransactionController implements ProtocolMessageHandler<ByteBuf> {

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
            connectionManager.closeTransactionsStream(stream);
            log.finest(String.format("Transactions stream closed for peer %s", stream.remotePeerId()));
            ProtocolMessageHandler.super.onClosed(stream);
        }

        @Override
        public void onException(Throwable cause) {
            connectionManager.closeTransactionsStream(stream);
            if (cause != null) {
                log.fine(String.format("Transactions Exception: %s", cause.getMessage()));
            } else {
                log.fine("Transactions Exception with unknown cause");
            }
            ProtocolMessageHandler.super.onException(cause);
        }
    }
}
