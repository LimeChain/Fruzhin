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
import java.math.BigInteger;
import java.util.logging.Level;

@Log
public class GrandpaEngine {
    private static final int HANDSHAKE_LENGTH = 1;
    private final ConnectionManager connectionManager = ConnectionManager.getInstance();
    private final SyncedState syncedState = SyncedState.getInstance();

    public void receiveRequest(byte[] msg, PeerId peerId, Stream stream) {
        GrandpaMessageType messageType = getGrandpaMessageType(msg);

        if (messageType == null) {
            log.log(Level.WARNING, String.format("Unknown grandpa message type \"%d\" from Peer %s", msg[0], peerId));
            return;
        }

        if (stream.isInitiator()) {
            handleInitiatorStreamMessage(msg, messageType, stream);
        } else {
            handleResponderStreamMessage(msg, messageType, stream);
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

    private GrandpaMessageType getGrandpaMessageType(byte[] msg) {
        if (msg.length == HANDSHAKE_LENGTH) {
            return GrandpaMessageType.HANDSHAKE;
        }
        return GrandpaMessageType.getByType(msg[0]);
    }

    private void handleHandshake(byte[] msg, PeerId peerId, Stream stream) {
        if (connectionManager.isGrandpaConnected(peerId)) {
            log.log(Level.INFO, "Received existing grandpa handshake from " + peerId);
        } else {
            connectionManager.addGrandpaStream(stream);
            connectionManager.getPeerInfo(peerId).setNodeRole(msg[0]);
            log.log(Level.INFO, "Received grandpa handshake from " + peerId);
        }
        writeHandshakeToStream(stream, peerId);
    }

    private void handleNeighbourMessage(byte[] msg, PeerId peerId) {
        ScaleCodecReader reader = new ScaleCodecReader(msg);
        NeighbourMessage neighbourMessage = reader.read(new NeighbourMessageScaleReader());
        log.log(Level.INFO, "Received neighbour message from Peer " + peerId + "\n" + neighbourMessage);
        if (syncedState.isWarpSyncFinished()
                && neighbourMessage.getSetId().compareTo(syncedState.getSetId()) > 0) {
            new Thread(() -> syncedState.updateSetData(neighbourMessage, peerId)).start();
        }
    }

    private void handleCommitMessage(byte[] msg, PeerId peerId) {
        ScaleCodecReader reader = new ScaleCodecReader(msg);
        CommitMessage commitMessage = reader.read(new CommitMessageScaleReader());
        if (isBlockAlreadyReached(commitMessage.getVote().getBlockNumber())) {
            log.log(Level.FINE, String.format("Received commit message for finalized block %d from peer %s",
                            commitMessage.getVote().getBlockNumber(), peerId));
            return;
        }

        log.log(Level.INFO, "Received commit message from peer " + peerId + "\n" + commitMessage);
        syncedState.syncCommit(commitMessage, peerId);
    }

    private boolean isBlockAlreadyReached(BigInteger blockNumber) {
        return syncedState.getLastFinalizedBlockNumber().compareTo(blockNumber) >= 0;
    }

    public void writeHandshakeToStream(Stream stream, PeerId peerId) {
        byte[] handshake = new byte[] {
                (byte) syncedState.getHandshake().getNodeRole()
        };

        log.log(Level.INFO, "Sending grandpa handshake to " + peerId);
        stream.writeAndFlush(handshake);
    }

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
