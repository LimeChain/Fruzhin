package com.limechain.network.protocol.grandpa;

import com.limechain.network.ConnectionManager;
import com.limechain.network.protocol.grandpa.messages.GrandpaMessageType;
import com.limechain.network.protocol.grandpa.messages.commit.CommitMessage;
import com.limechain.network.protocol.grandpa.messages.commit.CommitMessageScaleReader;
import com.limechain.network.protocol.grandpa.messages.neighbour.NeighbourMessage;
import com.limechain.network.protocol.grandpa.messages.neighbour.NeighbourMessageScaleReader;
import com.limechain.network.protocol.grandpa.messages.neighbour.NeighbourMessageScaleWriter;
import com.limechain.sync.warpsync.SyncedState;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleCodecWriter;
import io.libp2p.core.PeerId;
import io.libp2p.core.Stream;
import lombok.extern.java.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.logging.Level;

/**
 * Engine for handling transactions on GRANDPA streams.
 */
@Log
public class GrandpaEngine {
    private static final int HANDSHAKE_LENGTH = 1;

    private final ConnectionManager connectionManager = ConnectionManager.getInstance();
    private final SyncedState syncedState = SyncedState.getInstance();

    /**
     * Handles a received request as follows:
     *
     * <p><b>On streams we initiated:</b>  adds streams, where we receive a handshake message,
     * to peer's {@link com.limechain.network.dto.PeerInfo}, ignores all other message types.
     *
     * <p><b>On responder streams: </b>
     * <p>On handshake adds streams to non-connected peers and ignores for already connected ones. </p>
     * <p>On neighbour and commit messages, syncs received data using {@link SyncedState}. </p>
     * <p>Only logs other message types. </p>
     *
     * @param message received message as byre array
     * @param peerId peer id of sender
     * @param stream stream, where the request was received
     */
    public void receiveRequest(byte[] message, PeerId peerId, Stream stream) {
        GrandpaMessageType messageType = getGrandpaMessageType(message);

        if (messageType == null) {
            log.log(Level.WARNING,
                    String.format("Unknown grandpa message type \"%d\" from Peer %s", message[0], peerId));
            return;
        }

        if (stream.isInitiator()) {
            handleInitiatorStreamMessage(message, messageType, stream);
        } else {
            handleResponderStreamMessage(message, messageType, stream);
        }
    }

    private void handleInitiatorStreamMessage(byte[] message, GrandpaMessageType messageType, Stream stream) {
        PeerId peerId = stream.remotePeerId();
        if (messageType != GrandpaMessageType.HANDSHAKE) {
            stream.close();
            log.log(Level.WARNING, "Non handshake message on initiator grandpa stream from peer " + peerId);
            return;
        }
        connectionManager.addGrandpaStream(stream);
        log.log(Level.INFO, "Received grandpa handshake from " + peerId);
        writeNeighbourMessage(stream, peerId);
    }

    private void handleResponderStreamMessage(byte[] message, GrandpaMessageType messageType, Stream stream) {
        PeerId peerId = stream.remotePeerId();
        boolean connectedToPeer = connectionManager.isGrandpaConnected(peerId);

        if (!connectedToPeer && messageType != GrandpaMessageType.HANDSHAKE) {
            log.log(Level.WARNING, "No handshake for grandpa message from Peer " + peerId);
            stream.close();
            return;
        }

        switch (messageType) {
            case HANDSHAKE -> handleHandshake(message, peerId, stream);
            case VOTE -> log.log(Level.INFO, "Vote message received from Peer " + peerId);
            case COMMIT -> handleCommitMessage(message, peerId);
            case NEIGHBOUR -> handleNeighbourMessage(message, peerId);
            case CATCH_UP_REQUEST -> log.log(Level.INFO, "Catch up request received from Peer " + peerId);
            case CATCH_UP_RESPONSE -> log.log(Level.INFO, "Catch up response received from Peer " + peerId);
        }
    }

    private GrandpaMessageType getGrandpaMessageType(byte[] message) {
        if (message.length == HANDSHAKE_LENGTH) {
            return GrandpaMessageType.HANDSHAKE;
        }
        return GrandpaMessageType.getByType(message[0]);
    }

    private void handleHandshake(byte[] message, PeerId peerId, Stream stream) {
        if (connectionManager.isGrandpaConnected(peerId)) {
            log.log(Level.INFO, "Received existing grandpa handshake from " + peerId);
            stream.close();
        } else {
            connectionManager.addGrandpaStream(stream);
            connectionManager.getPeerInfo(peerId).setNodeRole(message[0]);
            log.log(Level.INFO, "Received grandpa handshake from " + peerId);
        }
        writeHandshakeToStream(stream, peerId);
    }

    private void handleNeighbourMessage(byte[] message, PeerId peerId) {
        ScaleCodecReader reader = new ScaleCodecReader(message);
        NeighbourMessage neighbourMessage = reader.read(new NeighbourMessageScaleReader());
        log.log(Level.INFO, "Received neighbour message from Peer " + peerId + "\n" + neighbourMessage);
        new Thread(() -> syncedState.syncNeighbourMessage(neighbourMessage, peerId)).start();
    }

    private void handleCommitMessage(byte[] message, PeerId peerId) {
        ScaleCodecReader reader = new ScaleCodecReader(message);
        CommitMessage commitMessage = reader.read(new CommitMessageScaleReader());
        syncedState.syncCommit(commitMessage, peerId);
    }

    /**
     * Sends host's GRANDPA handshake on given stream.
     *
     * @param stream stream to write message to
     * @param peerId peer to send to
     */
    public void writeHandshakeToStream(Stream stream, PeerId peerId) {
        byte[] handshake = new byte[] {
                (byte) syncedState.getHandshake().getNodeRole()
        };

        log.log(Level.INFO, "Sending grandpa handshake to " + peerId);
        stream.writeAndFlush(handshake);
    }

    /**
     * Sends host's GRANDPA neighbour message from {@link SyncedState} on given stream.
     *
     * @param stream stream to write message to
     * @param peerId peer to send to
     */
    public void writeNeighbourMessage(Stream stream, PeerId peerId) {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        try (ScaleCodecWriter writer = new ScaleCodecWriter(buf)) {
            writer.write(new NeighbourMessageScaleWriter(), syncedState.getNeighbourMessage());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        log.log(Level.INFO, "Sending neighbour message to peer " + peerId);
        stream.writeAndFlush(buf.toByteArray());
    }
}
