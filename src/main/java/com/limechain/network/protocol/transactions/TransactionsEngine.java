package com.limechain.network.protocol.transactions;

import com.limechain.exception.scale.ScaleEncodingException;
import com.limechain.network.ConnectionManager;
import com.limechain.network.protocol.transactions.scale.TransactionsReader;
import com.limechain.network.protocol.transactions.scale.TransactionsWriter;
import com.limechain.sync.warpsync.WarpSyncState;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleCodecWriter;
import io.libp2p.core.PeerId;
import io.libp2p.core.Stream;
import lombok.extern.java.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.logging.Level;

/**
 * Engine for handling transactions on Transactions streams.
 */
@Log
public class TransactionsEngine {
    private static final int HANDSHAKE_LENGTH = 1;

    private final ConnectionManager connectionManager = ConnectionManager.getInstance();

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
     * @param peerId  peer id of sender
     * @param stream  stream, where the request was received
     */
    public void receiveRequest(byte[] message, PeerId peerId, Stream stream) {
        if (message == null) {
            log.log(Level.WARNING,
                    String.format("Transactions message is null from Peer %s", peerId));
            return;
        }
        log.log(Level.INFO, "Transaction message length:" + message.length);

        if (stream.isInitiator()) {
            handleInitiatorStreamMessage(message, stream);
        } else {
            handleResponderStreamMessage(message, stream);
        }
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
        //TODO Send valid transactions to the peer we received a handshake from
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
            handleHandshake(peerId, stream);
        } else {
            handleTransactionMessage(message, peerId);
        }
    }

    private void handleHandshake(PeerId peerId, Stream stream) {
        if (connectionManager.isTransactionsConnected(peerId)) {
            log.log(Level.INFO, "Received existing transactions handshake from " + peerId);
            stream.close();
        } else {
            connectionManager.addTransactionsStream(stream);
            log.log(Level.INFO, "Received transactions handshake from " + peerId);
        }
        writeHandshakeToStream(stream, peerId);
    }

    private void handleTransactionMessage(byte[] message, PeerId peerId) {
        ScaleCodecReader reader = new ScaleCodecReader(message);
        byte[][] transactions = reader.read(new TransactionsReader());
        log.log(Level.INFO, "Received " + transactions.length + " transactions from Peer "
                + peerId);
        //TODO Add transactions to data
    }

    /**
     * Send our Transactions handshake on a given <b>initiator</b> stream.
     *
     * @param stream <b>initiator</b> stream to write the message to
     * @param peerId peer to send to
     */
    public void writeHandshakeToStream(Stream stream, PeerId peerId) {
        byte[] handshake = new byte[]{};

        log.log(Level.INFO, "Sending transactions handshake to " + peerId);
        stream.writeAndFlush(handshake);
    }

    /**
     * Send our Transactions message from {@link WarpSyncState} on a given <b>responder</b> stream.
     *
     * @param stream <b>responder</b> stream to write the message to
     * @param peerId peer to send to
     */
    public void writeTransactionsMessage(Stream stream, PeerId peerId) {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        try (ScaleCodecWriter writer = new ScaleCodecWriter(buf)) {
            writer.write(new TransactionsWriter(), new byte[][]{new byte[]{}, new byte[]{}});
        } catch (IOException e) {
            throw new ScaleEncodingException(e);
        }

        log.log(Level.INFO, "Sending transaction message to peer " + peerId);
        //TODO Send our transaction message
    }

    private boolean isHandshake(byte[] message) {
        return message.length == HANDSHAKE_LENGTH;
    }
}
