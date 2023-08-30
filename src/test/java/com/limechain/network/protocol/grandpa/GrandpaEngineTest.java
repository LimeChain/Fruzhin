package com.limechain.network.protocol.grandpa;

import com.limechain.network.ConnectionManager;
import com.limechain.network.dto.PeerInfo;
import com.limechain.network.protocol.blockannounce.scale.BlockAnnounceHandshake;
import com.limechain.network.protocol.grandpa.messages.commit.CommitMessage;
import com.limechain.network.protocol.grandpa.messages.commit.Vote;
import com.limechain.network.protocol.grandpa.messages.neighbour.NeighbourMessage;
import com.limechain.network.protocol.warp.dto.Precommit;
import com.limechain.sync.warpsync.SyncedState;
import io.emeraldpay.polkaj.types.Hash256;
import io.libp2p.core.PeerId;
import io.libp2p.core.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GrandpaEngineTest {

    private GrandpaEngine grandpaEngine;

    private static final NeighbourMessage NEIGHBOUR_MESSAGE =
            new NeighbourMessage(1, BigInteger.ONE, BigInteger.TWO, BigInteger.TEN);

    byte[] encodedNeighbourMessage
            = new byte[] {2, 1, 1, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 10, 0, 0, 0};

    private Stream stream;
    private PeerId peerId;
    private static final SyncedState SYNCED_STATE = mock(SyncedState.class);
    private static final ConnectionManager CONNECTION_MANAGER = mock(ConnectionManager.class);
    private static final Logger LOGGER = mock(Logger.class);
    private static final int ROLE = 2;

    @BeforeAll
    static void init() {
        mockStatic(SyncedState.class).when(SyncedState::getInstance).thenReturn(SYNCED_STATE);
        mockStatic(ConnectionManager.class).when(ConnectionManager::getInstance).thenReturn(CONNECTION_MANAGER);
        mockStatic(Logger.class).when(() -> Logger.getLogger(GrandpaEngine.class.getName())).thenReturn(LOGGER);
        BlockAnnounceHandshake handshake = mock(BlockAnnounceHandshake.class);
        when(SYNCED_STATE.getHandshake()).thenReturn(handshake);
        when(SYNCED_STATE.getNeighbourMessage()).thenReturn(NEIGHBOUR_MESSAGE);
        when(handshake.getNodeRole()).thenReturn(ROLE);
    }

    @BeforeEach
    void setup() {
        grandpaEngine = new GrandpaEngine();
        stream = mock(Stream.class);
        peerId = mock(PeerId.class);
        when(stream.remotePeerId()).thenReturn(peerId);
        when(peerId.toString()).thenReturn("P1");
    }

    @Test
    void receiveRequestWithUnknownGrandpaTypeShouldLogAndIgnore() {
        byte[] unknownTypeMessage = new byte[] {7, 1, 1, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 10, 0, 0, 0};

        grandpaEngine.receiveRequest(unknownTypeMessage, stream);

        verify(LOGGER).log(eq(Level.WARNING), contains("Unknown grandpa message type"));
    }

    // INITIATOR STREAM
    @Test
    void receiveNonHandshakeRequestOnInitiatorStreamShouldLogAndIgnore() {
        byte[] message = new byte[] {2, 1, 1, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 10, 0, 0, 0};
        when(stream.isInitiator()).thenReturn(true);

        grandpaEngine.receiveRequest(message, stream);

        verify(LOGGER).log(eq(Level.WARNING), contains("Non handshake message on initiator grandpa stream"));
    }

    @Test
    void receiveHandshakeOnInitiatorStreamShouldAddStreamToConnection() {
        byte[] message = new byte[] { 2 };
        when(stream.isInitiator()).thenReturn(true);

        grandpaEngine.receiveRequest(message, stream);

        verify(CONNECTION_MANAGER).addGrandpaStream(stream);
    }

    @Test
    void receiveHandshakeOnInitiatorStreamShouldSendNeighbourMessageBack() {
        byte[] message = new byte[] { 2 };
        when(stream.isInitiator()).thenReturn(true);

        grandpaEngine.receiveRequest(message, stream);

        verify(stream).writeAndFlush(encodedNeighbourMessage);
    }
    // RESPONDER STREAM
    @Test
    void receiveNonHandshakeRequestOnResponderStreamWhenNotConnectedShouldLogAndCloseStream() {
        byte[] message = new byte[] {2, 1, 1, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 10, 0, 0, 0};
        when(stream.isInitiator()).thenReturn(false);
        when(CONNECTION_MANAGER.isGrandpaConnected(peerId)).thenReturn(false);

        grandpaEngine.receiveRequest(message, stream);

        verify(LOGGER).log(eq(Level.WARNING), contains("No handshake for grandpa message"));
        verify(stream).close();
    }

    @Test
    void receiveHandshakeRequestOnResponderStreamWhenAlreadyConnectedShouldLogAndCloseStream() {
        byte[] message = new byte[] { 2 };
        when(stream.isInitiator()).thenReturn(false);
        when(CONNECTION_MANAGER.isGrandpaConnected(peerId)).thenReturn(true);

        grandpaEngine.receiveRequest(message, stream);

        verify(LOGGER).log(eq(Level.INFO), contains("Received existing grandpa handshake"));
        verify(stream).close();
    }

    @Test
    void receiveHandshakeRequestOnResponderStreamWhenNotConnectedShouldAddStreamToConnection() {
        byte[] message = new byte[] { 2 };
        when(stream.isInitiator()).thenReturn(false);
        when(CONNECTION_MANAGER.isGrandpaConnected(peerId)).thenReturn(false);
        when(CONNECTION_MANAGER.getPeerInfo(peerId)).thenReturn(mock(PeerInfo.class));

        grandpaEngine.receiveRequest(message, stream);

        verify(CONNECTION_MANAGER).addGrandpaStream(stream);
    }

    @Test
    void receiveHandshakeRequestOnResponderStreamWhenNotConnectedShouldSendHandshakeBack() {
        byte[] message = new byte[] { 2 };
        when(stream.isInitiator()).thenReturn(false);
        when(CONNECTION_MANAGER.isGrandpaConnected(peerId)).thenReturn(false);
        when(CONNECTION_MANAGER.getPeerInfo(peerId)).thenReturn(mock(PeerInfo.class));

        grandpaEngine.receiveRequest(message, stream);

        verify(stream).writeAndFlush(new byte[] { ROLE });
    }

    @Test
    void receiveCommitMessageOnResponderStreamWhenShouldSyncCommit() {
        byte[] message = new byte[] { 1, -77, 59, 0, 0, 0, 0, 0, 0, 37, 6, 0, 0, 0, 0, 0, 0, 71, 127, 101, -89, -9, -31,
                38, 117, 16, -19, 19, -47, 73, -43, -103, -3, 115, 35, -50, 58, 61, 118, -102, -107, 32, 47, -79, 60,
                85, 7, -72, 60, 8, 118, 4, 1, 0, 0 };
        CommitMessage commitMessage = new CommitMessage();
        commitMessage.setRoundNumber(BigInteger.valueOf(15283));
        commitMessage.setSetId(BigInteger.valueOf(1573));
        commitMessage.setVote(new Vote());
        commitMessage.getVote().setBlockHash(
                Hash256.from("0x477f65a7f7e1267510ed13d149d599fd7323ce3a3d769a95202fb13c5507b83c"));
        commitMessage.getVote().setBlockNumber(BigInteger.valueOf(17069576));
        commitMessage.setPrecommits(new Precommit[]{});

        when(stream.isInitiator()).thenReturn(false);
        when(CONNECTION_MANAGER.isGrandpaConnected(peerId)).thenReturn(true);

        grandpaEngine.receiveRequest(message, stream);

        verify(SYNCED_STATE).syncCommit(commitMessage, peerId);
    }

    @Test
    void receiveNeighbourMessageOnResponderStreamWhenShouldSyncNeighbourMessage() {
        byte[] message = new byte[] { 2, 1, -24, 60, 0, 0, 0, 0, 0, 0, 37, 6, 0, 0, 0, 0, 0, 0, -37, 118, 4, 1 };
        NeighbourMessage neighbourMessage = new NeighbourMessage(1, BigInteger.valueOf(15592),
                BigInteger.valueOf(1573), BigInteger.valueOf(17069787));

        when(stream.isInitiator()).thenReturn(false);
        when(CONNECTION_MANAGER.isGrandpaConnected(peerId)).thenReturn(true);

        grandpaEngine.receiveRequest(message, stream);

        verify(SYNCED_STATE).syncNeighbourMessage(neighbourMessage, peerId);
    }
    @Test
    void receiveVoteMessageOnResponderStreamShouldLogAndIgnore() {
        byte[] message = new byte[] { 0, 2, 3 };
        when(stream.isInitiator()).thenReturn(false);
        when(CONNECTION_MANAGER.isGrandpaConnected(peerId)).thenReturn(true);

        grandpaEngine.receiveRequest(message, stream);

        verify(LOGGER).log(eq(Level.INFO), contains("Vote message received"));
    }

    @Test
    void receiveCatchUpRequestMessageOnResponderStreamShouldLogAndIgnore() {
        byte[] message = new byte[] { 3, 2, 3 };
        when(stream.isInitiator()).thenReturn(false);
        when(CONNECTION_MANAGER.isGrandpaConnected(peerId)).thenReturn(true);

        grandpaEngine.receiveRequest(message, stream);

        verify(LOGGER).log(eq(Level.INFO), contains("Catch up request received"));
    }

    @Test
    void receiveCatchUpResponseMessageOnResponderStreamShouldLogAndIgnore() {
        byte[] message = new byte[] { 4, 2, 3 };
        when(stream.isInitiator()).thenReturn(false);
        when(CONNECTION_MANAGER.isGrandpaConnected(peerId)).thenReturn(true);

        grandpaEngine.receiveRequest(message, stream);

        verify(LOGGER).log(eq(Level.INFO), contains("Catch up response received"));
    }

    // WRITE
    @Test
    void writeHandshakeToStream() {
        byte[] expected = new byte[] { ROLE };

        grandpaEngine.writeHandshakeToStream(stream, peerId);

        verify(stream).writeAndFlush(expected);
    }

    @Test
    void writeNeighbourMessage() {
        grandpaEngine.writeNeighbourMessage(stream, peerId);

        verify(stream).writeAndFlush(encodedNeighbourMessage);
    }
}