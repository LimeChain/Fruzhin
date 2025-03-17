package com.limechain.network.protocol.beefy;

import com.limechain.network.ConnectionManager;
import com.limechain.network.protocol.base.BaseEngine;
import com.limechain.network.protocol.beefy.messages.BeefyMessageType;
import com.limechain.network.protocol.beefy.messages.vote.VoteMessageScaleReader;
import com.limechain.network.protocol.grandpa.messages.vote.SignedMessageScaleReader;
import com.limechain.rpc.server.AppBean;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
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
            case SIGNED_COMMITMENT -> handleSignedCommitmentMessage(message, peerId);
        }
    }

    private void handleVoteMessage(byte[] message, PeerId peerId) {
        log.fine("We are here");
        ScaleCodecReader reader = new ScaleCodecReader(message);
        reader.read(VoteMessageScaleReader.getInstance());

    }

    private void handleSignedCommitmentMessage(byte[] message, PeerId peerId) {
        log.fine("We are here");
        ScaleCodecReader reader = new ScaleCodecReader(message);
        reader.read(SignedMessageScaleReader.getInstance());
    }

    private BeefyMessageType getBeefyMessageType(byte[] message) {
        return message.length == HANDSHAKE_LENGTH
                ? BeefyMessageType.HANDSHAKE
                : BeefyMessageType.getByType(message[0]);
    }
}
