package com.limechain.network.protocol.grandpa;

import com.limechain.config.HostConfig;
import com.limechain.consensus.grandpa.GrandpaService;
import com.limechain.exception.scale.ScaleEncodingException;
import com.limechain.network.ConnectionManager;
import com.limechain.network.protocol.base.BaseEngine;
import com.limechain.network.protocol.blockannounce.NodeRole;
import com.limechain.network.protocol.grandpa.messages.GrandpaMessageType;
import com.limechain.network.protocol.grandpa.messages.catchup.req.CatchUpReqMessage;
import com.limechain.network.protocol.grandpa.messages.catchup.req.CatchUpReqMessageScaleReader;
import com.limechain.network.protocol.grandpa.messages.catchup.res.CatchUpResMessage;
import com.limechain.network.protocol.grandpa.messages.catchup.res.CatchUpResMessageScaleReader;
import com.limechain.network.protocol.grandpa.messages.commit.CommitMessage;
import com.limechain.network.protocol.grandpa.messages.commit.CommitMessageScaleReader;
import com.limechain.network.protocol.grandpa.messages.neighbour.NeighbourMessage;
import com.limechain.network.protocol.grandpa.messages.neighbour.NeighbourMessageScaleReader;
import com.limechain.network.protocol.grandpa.messages.neighbour.NeighbourMessageScaleWriter;
import com.limechain.network.protocol.grandpa.messages.vote.VoteMessage;
import com.limechain.network.protocol.grandpa.messages.vote.VoteMessageScaleReader;
import com.limechain.network.protocol.message.ProtocolMessageBuilder;
import com.limechain.rpc.server.AppBean;
import com.limechain.state.AbstractState;
import com.limechain.sync.SyncMode;
import com.limechain.sync.warpsync.WarpSyncState;
import com.limechain.utils.scale.ScaleUtils;
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
public class GrandpaEngine implements BaseEngine {

    private static final int HANDSHAKE_LENGTH = 1;

    protected ConnectionManager connectionManager;
    protected GrandpaMessageHandler grandpaMessageHandler;
    protected HostConfig hostConfig;

    public GrandpaEngine() {
        connectionManager = ConnectionManager.getInstance();
        grandpaMessageHandler = AppBean.getBean(GrandpaMessageHandler.class);
        hostConfig = AppBean.getBean(HostConfig.class);
    }

    @Override
    public void handleHandshake(byte[] message, PeerId peerId, Stream stream) {
        if (connectionManager.isGrandpaConnected(peerId)) {
            log.log(Level.INFO, "Received existing grandpa handshake from " + peerId);
            stream.close();
        } else {
            connectionManager.addGrandpaStream(stream);
            connectionManager.getPeerInfo(peerId).setNodeRole(message[0]);
            log.log(Level.INFO, "Received grandpa handshake from " + peerId);
            writeHandshakeToStream(stream, peerId);
        }
    }

    /**
     * Handles an incoming request as follows:
     *
     * <p><b>On streams we initiated:</b>  adds streams, where we receive a handshake message,
     * to initiator streams in peer's {@link com.limechain.network.dto.PeerInfo} , ignores all other message types.
     *
     * <p><b>On responder stream: </b>
     * <p>If message payload contains a valid handshake, adds the stream when the peer is not connected already,
     * ignore otherwise. </p>
     * <p>On neighbour and commit messages, syncs received data using {@link WarpSyncState}. </p>
     * <p>Logs and ignores other message types. </p>
     *
     * @param message received message as byre array
     * @param stream  stream, where the request was received
     */
    @Override
    public void receiveRequest(byte[] message, Stream stream) {
        GrandpaMessageType messageType = getGrandpaMessageType(message);

        if (messageType == null) {
            log.log(Level.WARNING, String.format("Unknown grandpa message type \"%d\" from Peer %s",
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
     * Send our GRANDPA handshake on a given <b>initiator</b> stream.
     *
     * @param stream <b>initiator</b> stream to write the message to
     * @param peerId peer to send to
     */
    @Override
    public void writeHandshakeToStream(Stream stream, PeerId peerId) {
        NodeRole nodeRole = hostConfig.getNodeRole();

        byte[] handshake = new byte[]{
                nodeRole.getValue().byteValue()
        };

        log.log(Level.INFO, "Sending grandpa handshake to " + peerId);
        stream.writeAndFlush(handshake);
    }

    /**
     * Send our GRANDPA neighbour message from {@link WarpSyncState} on a given <b>responder</b> stream.
     *
     * @param stream <b>responder</b> stream to write the message to
     * @param peerId peer to send to
     */
    public void writeNeighbourMessage(Stream stream, PeerId peerId) {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        try (ScaleCodecWriter writer = new ScaleCodecWriter(buf)) {
            writer.write(NeighbourMessageScaleWriter.getInstance(), ProtocolMessageBuilder.buildNeighbourMessage());
        } catch (IOException e) {
            throw new ScaleEncodingException(e);
        }

        log.log(Level.FINE, "Sending neighbour message to Peer " + peerId);
        stream.writeAndFlush(buf.toByteArray());
    }

    /**
     * Send our GRANDPA commit message from {@link GrandpaService} on a given <b>responder</b> stream.
     *
     * @param stream               <b>responder</b> stream to write the message to
     * @param encodedCommitMessage scale encoded CommitMessage object
     */
    public void writeCommitMessage(Stream stream, byte[] encodedCommitMessage) {
        log.log(Level.FINE, "Sending commit message to Peer " + stream.remotePeerId());
        stream.writeAndFlush(encodedCommitMessage);
    }

    /**
     * Send our GRANDPA catch-up request message on a given <b>responder</b> stream.
     *
     * @param stream                   <b>responder</b> stream to write the message to
     * @param encodedCatchUpReqMessage scale encoded CatchUpRequestMessage object
     */
    public void writeCatchUpRequest(Stream stream, byte[] encodedCatchUpReqMessage) {
        log.log(Level.FINE, "Sending catch up request to Peer " + stream.remotePeerId());
        stream.writeAndFlush(encodedCatchUpReqMessage);
    }

    /**
     * Send our GRANDPA catch-up response message on a given <b>responder</b> stream.
     *
     * @param stream                   <b>responder</b> stream to write the message to
     * @param encodedCatchUpResMessage scale encoded CatchUpResMessage object
     */
    public void writeCatchUpResponse(Stream stream, byte[] encodedCatchUpResMessage) {
        log.log(Level.FINE, "Sending catch up response to Peer " + stream.remotePeerId());
        stream.writeAndFlush(encodedCatchUpResMessage);
    }

    /**
     * Send our GRANDPA vote message from {@link GrandpaService} on a given <b>responder</b> stream.
     *
     * @param stream             <b>responder</b> stream to write the message to
     * @param encodedVoteMessage scale encoded VoteMessage object
     */
    public void writeVoteMessage(Stream stream, byte[] encodedVoteMessage) {
        log.log(Level.FINE, "Sending vote message to peer " + stream.remotePeerId());
        stream.writeAndFlush(encodedVoteMessage);
    }

    private void handleInitiatorStreamMessage(GrandpaMessageType messageType, Stream stream) {
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
            case VOTE -> handleVoteMessage(message, peerId);
            case COMMIT -> handleCommitMessage(message, peerId);
            case NEIGHBOUR -> handleNeighbourMessage(message, stream);
            case CATCH_UP_REQUEST, CATCH_UP_RESPONSE -> handleCatchUpMessage(message, messageType, peerId);
        }
    }

    private void handleCatchUpMessage(byte[] message, GrandpaMessageType messageType, PeerId peerId) {
        if (!AbstractState.isActiveAuthority() || !connectionManager.checkIfPeerIsAuthorNode(peerId)) {
            return;
        }

        if (messageType.equals(GrandpaMessageType.CATCH_UP_REQUEST)) {
            handleCatchupRequestMessage(message, peerId);
        } else {
            handleCatchupResponseMessage(message, peerId);
        }
    }

    private GrandpaMessageType getGrandpaMessageType(byte[] message) {
        if (message.length == HANDSHAKE_LENGTH) {
            return GrandpaMessageType.HANDSHAKE;
        }
        return GrandpaMessageType.getByType(message[0]);
    }

    private void handleNeighbourMessage(byte[] message, Stream stream) {

        NeighbourMessage neighbourMessage = ScaleUtils.Decode.decode(
                message,
                NeighbourMessageScaleReader.getInstance()
        );

        log.log(Level.FINE, "Received neighbour message from Peer " + stream.remotePeerId() + "\n" + neighbourMessage);
        // TODO: We need to actually update our peer's infos on each message.
        writeNeighbourMessage(stream, stream.remotePeerId());

        if (SyncMode.HEAD.equals(AbstractState.getSyncMode()) && AbstractState.isActiveAuthority()) {
            grandpaMessageHandler.initiateAndSendCatchUpRequest(neighbourMessage, stream.remotePeerId());
        }
    }

    private void handleVoteMessage(byte[] message, PeerId peerId) {
        VoteMessage voteMessage = ScaleUtils.Decode.decode(message, VoteMessageScaleReader.getInstance());
        log.log(Level.INFO, "Received vote message from Peer " + peerId + "\n" + voteMessage);
        grandpaMessageHandler.handleVoteMessage(voteMessage);
    }

    private void handleCommitMessage(byte[] message, PeerId peerId) {
        CommitMessage commitMessage = ScaleUtils.Decode.decode(message, CommitMessageScaleReader.getInstance());
        log.log(Level.INFO, "Received commit message from Peer " + peerId +
                " " + commitMessage.getRoundNumber() +
                " " + commitMessage.getSetId());

        grandpaMessageHandler.handleCommitMessage(commitMessage, peerId);
    }

    private void handleCatchupRequestMessage(byte[] message, PeerId peerId) {

        CatchUpReqMessage catchUpReqMessage = ScaleUtils.Decode.decode(
                message,
                CatchUpReqMessageScaleReader.getInstance()
        );

        log.log(Level.INFO, "Received catch up request message from Peer " + peerId + "\n" + catchUpReqMessage);

        grandpaMessageHandler.initiateAndSendCatchUpResponse(peerId, catchUpReqMessage, connectionManager::getPeerIds);
    }

    private void handleCatchupResponseMessage(byte[] message, PeerId peerId) {

        CatchUpResMessage catchUpResMessage = ScaleUtils.Decode.decode(
                message,
                CatchUpResMessageScaleReader.getInstance()
        );

        log.log(Level.INFO, "Received catch up response message from Peer " + peerId + "\n" + catchUpResMessage);

        grandpaMessageHandler.handleCatchUpResponse(peerId, catchUpResMessage, connectionManager::getPeerIds);
    }
}
