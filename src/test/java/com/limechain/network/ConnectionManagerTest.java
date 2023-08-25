package com.limechain.network;

import com.limechain.network.dto.PeerInfo;
import com.limechain.network.dto.ProtocolStreams;
import com.limechain.network.protocol.blockannounce.scale.BlockAnnounceMessage;
import com.limechain.network.protocol.warp.dto.BlockHeader;
import io.emeraldpay.polkaj.types.Hash256;
import io.libp2p.core.PeerId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConnectionManagerTest {
    ConnectionManager connectionManager;
    PeerId peerId = mock(PeerId.class);
    PeerInfo peerInfo;

    @BeforeEach
    void setUp() {
        connectionManager = new ConnectionManager();
        peerInfo = mock(PeerInfo.class);
    }

    @Test
    void updatePeerShouldDoNothingIfNotBestBlock() {
        connectionManager.peers.put(peerId, peerInfo);
        when(peerInfo.getLatestBlock()).thenReturn(BigInteger.ZERO);
        BlockAnnounceMessage message = mock(BlockAnnounceMessage.class);
        when(message.isBestBlock()).thenReturn(false);
        BlockHeader header = mock(BlockHeader.class);
        when(message.getHeader()).thenReturn(header);
        when(header.getBlockNumber()).thenReturn(BigInteger.TEN);

        connectionManager.updatePeer(peerId, message);
    }

    @Test
    void updatePeerShouldUpdateWhenBestBlock() {
        connectionManager.peers.put(peerId, peerInfo);
        when(peerInfo.getLatestBlock()).thenReturn(BigInteger.ZERO);
        BlockAnnounceMessage message = mock(BlockAnnounceMessage.class);
        when(message.isBestBlock()).thenReturn(true);
        BlockHeader header = mock(BlockHeader.class);
        when(message.getHeader()).thenReturn(header);
        when(header.getBlockNumber()).thenReturn(BigInteger.TEN);
        byte[] hash = new byte[32];
        Arrays.fill( hash, (byte) 3 );
        when(header.getHash()).thenReturn(hash);

        connectionManager.updatePeer(peerId, message);

        verify(peerInfo).setBestBlock(BigInteger.TEN);
        verify(peerInfo).setBestBlockHash(new Hash256(hash));
    }

    @Test
    void isBlockAnnounceConnected() {
        connectionManager.peers.put(peerId, peerInfo);
        when(peerInfo.getBlockAnnounceStreams()).thenReturn(mock(ProtocolStreams.class));
        assertTrue(connectionManager.isBlockAnnounceConnected(peerId));
    }

    @Test
    void isGrandpaConnected() {
        connectionManager.peers.put(peerId, peerInfo);
        when(peerInfo.getGrandpaStreams()).thenReturn(mock(ProtocolStreams.class));
        assertTrue(connectionManager.isGrandpaConnected(peerId));
    }

    @Test
    void isBlockAnnounceConnectedAndGrandpaConnectedReturnFalseWhenNotInPeers() {
        connectionManager.peers.clear();
        assertFalse(connectionManager.isBlockAnnounceConnected(peerId));
        assertFalse(connectionManager.isGrandpaConnected(peerId));
    }

    @Test
    void getPeerIds() {
        assertSame(connectionManager.peers.keySet(), connectionManager.getPeerIds());
    }
}