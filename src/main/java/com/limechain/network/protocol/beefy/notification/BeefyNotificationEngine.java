package com.limechain.network.protocol.beefy.notification;

import com.limechain.network.ConnectionManager;
import com.limechain.network.protocol.base.BaseEngine;
import com.limechain.network.protocol.beefy.BeefyMessageHandler;
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

/**
 * Engine for handling transactions on BEEFY notification streams
 */
@Log
public class BeefyNotificationEngine implements BaseEngine {

    private static final int HANDSHAKE_LENGTH = 1;

    protected ConnectionManager connectionManager;
    protected BeefyMessageHandler beefyMessageHandler;

    public BeefyNotificationEngine() {
        connectionManager = ConnectionManager.getInstance();
        beefyMessageHandler = AppBean.getBean(BeefyMessageHandler.class);
    }

    @Override
    public void handleHandshake(byte[] message, PeerId peerId, Stream stream) {

        if (connectionManager.isBeefyConnected(peerId)) {
            log.info(String.format("Received existing beefy handshake from %s", peerId));
            stream.close();
        } else {
            connectionManager.addBeefyStream(stream);
            connectionManager.getPeerInfo(peerId).setNodeRole(message[0]);
            log.info(String.format("Received beefy handshake from %s", peerId));
            writeHandshakeToStream(stream, peerId);
        }
    }

    @Override
    public void receiveRequest(byte[] message, Stream stream) {

        BeefyMessageType messageType = getBeefyMessageType(message);

        if (messageType == null) {
            log.warning(String.format("Unknown beefy message type \"%d\" from Peer %s",
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
        log.info(String.format("Sending beefy handshake to %s", peerId));
        stream.writeAndFlush(handshake);
    }

    /**
     * Sends a BEEFY message over the given responder stream.
     * <p>
     * Since both vote messages and signed commitments are transmitted over the same stream,
     * there's no need for separate logic after the message is encoded.
     * </p>
     *
     * @param stream         the responder stream to write the message to.
     * @param encodedMessage the scale encoded BEEFY message to send (either a vote message or a signed commitment).
     */
    public void writeMessage(Stream stream, byte[] encodedMessage) {
        BeefyMessageType type = BeefyMessageType.getByType(encodedMessage[0]);
        log.fine(String.format("Sending beefy %s to peer %s", type, stream.remotePeerId()));
        stream.writeAndFlush(encodedMessage);
    }

    private void handleInitiatorStreamMessage(BeefyMessageType messageType, Stream stream) {

        PeerId peerId = stream.remotePeerId();
        if (messageType != BeefyMessageType.HANDSHAKE) {
            stream.close();
            log.warning(String.format("Non handshake message on initiator beefy steam from peer %s", peerId));
            return;
        }

        connectionManager.addBeefyStream(stream);
        log.info(String.format("Received beefy handshake from %s", peerId));
    }

    private void handleResponderStreamMessage(byte[] message, BeefyMessageType messageType, Stream stream) {
        PeerId peerId = stream.remotePeerId();
        boolean connectedToPeer = connectionManager.isBeefyConnected(peerId);

        if (!connectedToPeer && messageType != BeefyMessageType.HANDSHAKE) {
            log.warning(String.format("No handshake for beefy message from peer %s", peerId));
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
        log.fine("Beefy: Received vote message from Peer " + peerId + "\n" + voteMessage);
        beefyMessageHandler.handleVoteMessage(voteMessage);
    }

    private void handleJustificationMessage(byte[] message, PeerId peerId) {
        SignedCommitment signedCommitment = ScaleUtils.Decode.decode(message, SignedCommitmentScaleReader.getInstance());
        log.fine("Beefy: Received justification from Peer " + peerId + "\n" + signedCommitment);
        beefyMessageHandler.handleSignedCommitment(signedCommitment);
    }

    private BeefyMessageType getBeefyMessageType(byte[] message) {
        return message.length == HANDSHAKE_LENGTH
                ? BeefyMessageType.HANDSHAKE
                : BeefyMessageType.getByType(message[0]);
    }
}
