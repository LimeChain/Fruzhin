package com.limechain.network.protocol.beefy;

import com.limechain.network.ConnectionManager;
import com.limechain.network.protocol.base.BaseEngine;
import com.limechain.network.protocol.beefy.messages.BeefyMessageType;
import com.limechain.network.protocol.beefy.messages.justification.SignedCommitment;
import com.limechain.network.protocol.beefy.messages.justification.SignedCommitmentScaleReader;
import com.limechain.network.protocol.beefy.messages.vote.BeefyVoteMessage;
import com.limechain.network.protocol.beefy.messages.vote.BeefyVoteMessageScaleReader;
import com.limechain.rpc.server.AppBean;
import com.limechain.utils.scale.ScaleUtils;
import io.libp2p.core.PeerId;
import io.libp2p.core.Stream;
import lombok.extern.java.Log;

import java.util.logging.Level;

/**
 * Engine for handling transactions on BEEFY streams
 */
@Log
public class BeefyEngine implements BaseEngine {

    private static final int HANDSHAKE_LENGTH = 1;

    protected ConnectionManager connectionManager;
    protected BeefyMessageHandler beefyMessageHandler;

    public BeefyEngine() {
        connectionManager = ConnectionManager.getInstance();
        beefyMessageHandler = AppBean.getBean(BeefyMessageHandler.class);
    }

    @Override
    public void handleHandshake(byte[] message, PeerId peerId, Stream stream) {

        if (connectionManager.isBeefyConnected(peerId)) {
            log.log(Level.INFO, "Received existing beefy handshake from " + peerId);
            stream.close();
        } else {
            connectionManager.addBeefyStream(stream);
            connectionManager.getPeerInfo(peerId).setNodeRole(message[0]);
            log.log(Level.INFO, "Received beefy handshake from " + peerId);
            writeHandshakeToStream(stream, peerId);
        }
    }

    @Override
    public void receiveRequest(byte[] message, Stream stream) {

        BeefyMessageType messageType = getBeefyMessageType(message);

        if (messageType == null) {
            log.log(Level.WARNING, String.format("Unknown beefy message type \"%d\" from Peer %s",
                    message[0], stream.remotePeerId()));
            return;
        }

        if (stream.isInitiator()) {
            handleInitiatorStreamMessage(messageType, stream);
        } else {
            handleResponderStreamMessage(message, messageType, stream);
        }
    }

    /**
     * Send our BEEFY handshake on a given initiator stream
     *
     * @param stream initiator stream to write the message to
     * @param peerId peer to send to
     */
    @Override
    public void writeHandshakeToStream(Stream stream, PeerId peerId) {
        byte[] handshake = new byte[]{};
        log.log(Level.INFO, "Sending beefy handshake to " + peerId);
        stream.writeAndFlush(handshake);
    }

    /**
     * Sends a BEEFY message over the given responder stream.
     * <p>
     * Since both vote messages and signed commitments are transmitted over the same stream,
     * there's no need for separate logic after the message is encoded.
     * </p>
     * @param stream the responder stream to write the message to.
     * @param encodedMessage the scale encoded BEEFY message to send (either a vote message or a signed commitment).
     */
    public void writeMessage(Stream stream, byte[] encodedMessage) {
        log.log(Level.FINE, "Sending beefy message to peer " + stream.remotePeerId());
        stream.writeAndFlush(encodedMessage);
    }

    private void handleInitiatorStreamMessage(BeefyMessageType messageType, Stream stream) {

        PeerId peerId = stream.remotePeerId();
        if (messageType != BeefyMessageType.HANDSHAKE) {
            stream.close();
            log.log(Level.WARNING, "Non handshake message on initiator beefy steam from peer " + peerId);
            return;
        }

        connectionManager.addBeefyStream(stream);
        log.log(Level.INFO, "Received beefy handshake from " + peerId);
    }

    private void handleResponderStreamMessage(byte[] message, BeefyMessageType messageType, Stream stream) {
        PeerId peerId = stream.remotePeerId();
        boolean connectedToPeer = connectionManager.isBeefyConnected(peerId);

        if (!connectedToPeer && messageType != BeefyMessageType.HANDSHAKE) {
            log.log(Level.WARNING, "No handshake for beefy message from peer " + peerId);
            stream.close();
            return;
        }

        switch (messageType) {
            case HANDSHAKE -> handleHandshake(message, peerId, stream);
            case VOTE -> handleVoteMessage(message, peerId);
            case JUSTIFICATION -> handleJustificationMessage(message, peerId);
        }
    }

    private void handleVoteMessage(byte[] message, PeerId peerId) {
        BeefyVoteMessage voteMessage = ScaleUtils.Decode.decode(message, BeefyVoteMessageScaleReader.getInstance());
        log.info("Beefy: Received vote message from Peer " + peerId + "\n" + voteMessage);
        beefyMessageHandler.handleVoteMessage(voteMessage);
    }

    private void handleJustificationMessage(byte[] message, PeerId peerId) {
        SignedCommitment signedCommitment = ScaleUtils.Decode.decode(message, SignedCommitmentScaleReader.getInstance());
        log.info("Beefy: Received justification from Peer " + peerId + "\n" + signedCommitment);
        beefyMessageHandler.handleSignedCommitment(signedCommitment);
    }

    private BeefyMessageType getBeefyMessageType(byte[] message) {
        return message.length == HANDSHAKE_LENGTH
                ? BeefyMessageType.HANDSHAKE
                : BeefyMessageType.getByType(message[0]);
    }
}
