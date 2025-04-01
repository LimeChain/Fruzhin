package com.limechain.network.protocol.transaction;

import com.limechain.network.ConnectionManager;
import com.limechain.network.protocol.base.BaseEngine;
import com.limechain.network.protocol.transaction.scale.TransactionReader;
import com.limechain.rpc.server.AppBean;
import com.limechain.state.AbstractState;
import com.limechain.sync.SyncMode;
import com.limechain.sync.warpsync.WarpSyncState;
import com.limechain.transaction.TransactionProcessor;
import com.limechain.transaction.dto.ExtrinsicArray;
import com.limechain.utils.async.AsyncExecutor;
import com.limechain.utils.scale.ScaleUtils;
import io.libp2p.core.PeerId;
import io.libp2p.core.Stream;
import lombok.extern.java.Log;

import java.util.logging.Level;

/**
 * Engine for handling transactions on Transactions streams.
 */
@Log
public class TransactionEngine implements BaseEngine {

    /**
     * Equals to the number of different messages we can receive excluding handshake.
     */
    private static final AsyncExecutor TRANSACTION_EXECUTOR = AsyncExecutor.withSingleThread();
    private static final int HANDSHAKE_LENGTH = 1;

    private final ConnectionManager connectionManager;
    private final TransactionProcessor transactionProcessor;

    public TransactionEngine() {
        connectionManager = ConnectionManager.getInstance();
        transactionProcessor = AppBean.getBean(TransactionProcessor.class);
    }

    @Override
    public void handleHandshake(byte[] message, PeerId peerId, Stream stream) {
        if (connectionManager.isTransactionsConnected(peerId)) {
            log.log(Level.INFO, "Received existing transactions handshake from " + peerId);
            stream.close();
        }

        connectionManager.addTransactionsStream(stream);
        log.log(Level.INFO, "Received transactions handshake from " + peerId);

        writeHandshakeToStream(stream, peerId);
    }

    /**
     * Handles an incoming request as follows:
     *
     * <p><b>On streams we initiated:</b>  adds streams, where we receive a handshake message,
     * to initiator streams in peer's {@link com.limechain.network.dto.PeerInfo}, ignores all other message types.
     *
     * <p><b>On responder stream: </b>
     * <p>If message payload contains a valid handshake, adds the stream when the peer is not connected already,
     * ignore otherwise. </p>
     * <p>On transactions messages {@link WarpSyncState}: </p>
     * <p>Logs and ignores other message types.</p>
     *
     * @param message received message as byre array
     * @param stream  stream, where the request was received
     */
    @Override
    public void receiveRequest(byte[] message, Stream stream) {
        if (message == null || message.length == 0) {
            log.log(Level.WARNING,
                    String.format("Transactions message is null from Peer %s", stream.remotePeerId()));
            return;
        }
        log.log(Level.FINE, "Transaction message length:" + message.length);

        if (stream.isInitiator()) {
            handleInitiatorStreamMessage(message, stream);
        } else {
            handleResponderStreamMessage(message, stream);
        }
    }

    /**
     * Send our Transactions handshake on a given <b>initiator</b> stream.
     *
     * @param stream <b>initiator</b> stream to write the message to
     * @param peerId peer to send to
     */
    @Override
    public void writeHandshakeToStream(Stream stream, PeerId peerId) {
        byte[] handshake = new byte[]{};
        log.log(Level.INFO, "Sending transactions handshake to " + peerId);
        stream.writeAndFlush(handshake);
    }

    /**
     * Send our Transactions message from {@link WarpSyncState} on a given <b>responder</b> stream.
     *
     * @param stream                    <b>responder</b> stream to write the message to
     * @param encodedTransactionMessage scale encoded transaction message
     */
    public void writeTransactionsMessage(Stream stream, byte[] encodedTransactionMessage) {
        log.log(Level.INFO, "Sending transaction message to peer " + stream.remotePeerId());
        stream.writeAndFlush(encodedTransactionMessage);
    }

    private void handleInitiatorStreamMessage(byte[] message, Stream stream) {
        PeerId peerId = stream.remotePeerId();

        if (!isHandshake(message)) {
            stream.close();
            log.log(Level.WARNING, "Non handshake message on initiator transactions stream from peer " + peerId);
            return;
        }

        connectionManager.addTransactionsStream(stream);
        log.log(Level.INFO, "Received transactions handshake from " + peerId);
        stream.writeAndFlush(new byte[]{});
    }

    private void handleResponderStreamMessage(byte[] message, Stream stream) {
        PeerId peerId = stream.remotePeerId();
        boolean connectedToPeer = connectionManager.isTransactionsConnected(peerId);

        if (!connectedToPeer && !isHandshake(message)) {
            log.log(Level.WARNING, "No handshake for transactions message from Peer " + peerId);
            stream.close();
            return;
        }

        if (isHandshake(message)) {
            handleHandshake(message, peerId, stream);
        } else {
            TRANSACTION_EXECUTOR.executeAndForget(() -> handleTransactionMessage(message, stream));
        }
    }

    private void handleTransactionMessage(byte[] message, Stream stream) {

        if (!SyncMode.HEAD.equals(AbstractState.getSyncMode())) {
            log.fine("Skipping transaction message before we reach head of chain.");
            return;
        }

        ExtrinsicArray transactions = ScaleUtils.Decode.decode(message, TransactionReader.getInstance());
        log.log(Level.FINE, "Received " + transactions.getExtrinsics().length + " transactions from Peer "
                + stream.remotePeerId());

        transactionProcessor.handleExternalTransactions(transactions.getExtrinsics(), stream.remotePeerId());
    }

    private boolean isHandshake(byte[] message) {
        return message.length == HANDSHAKE_LENGTH;
    }
}
