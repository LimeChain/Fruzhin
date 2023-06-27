package com.limechain.network;

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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ConnectionManagerTest {
    ConnectionManager connectionManager;
    PeerId peerId = mock(PeerId.class);
    PeerInfo peerInfo = mock(PeerInfo.class);

    @BeforeEach
    void setUp() {
        connectionManager = new ConnectionManager();
    }

    @Test
    void addPeer() {
        connectionManager.addPeer(peerId, peerInfo);

        assertSame(peerInfo, connectionManager.peers.get(peerId));
    }

    @Test
    void removePeer() {
        connectionManager.peers.put(peerId, peerInfo);
        connectionManager.removePeer(peerId);

        assertFalse(connectionManager.peers.containsKey(peerId));
    }

    @Test
    void updatePeerShouldDoNothingIfNotBestBlock() {
        connectionManager.peers.put(peerId, peerInfo);
        BlockAnnounceMessage message = mock(BlockAnnounceMessage.class);
        when(message.isBestBlock()).thenReturn(false);

        connectionManager.updatePeer(peerId, message);
        verifyNoInteractions(peerInfo);
    }

    @Test
    void updatePeerShouldUpdateWhenBestBlock() {
        connectionManager.peers.put(peerId, peerInfo);
        BlockAnnounceMessage message = mock(BlockAnnounceMessage.class);
        when(message.isBestBlock()).thenReturn(true);
        BlockHeader header = mock(BlockHeader.class);
        when(message.getHeader()).thenReturn(header);
        when(header.getBlockNumber()).thenReturn(BigInteger.TEN);
        byte[] hash = new byte[32];
        Arrays.fill( hash, (byte) 3 );
        when(header.getHash()).thenReturn(hash);

        connectionManager.updatePeer(peerId, message);

        verify(peerInfo).setBestBlock("10");
        verify(peerInfo).setBestBlockHash(new Hash256(hash));
    }

    @Test
    void isConnected() {
        connectionManager.peers.put(peerId, peerInfo);

        assertTrue(connectionManager.isConnected(peerId));
    }

    @Test
    void getPeerIds() {
        assertSame(connectionManager.peers.keySet(), connectionManager.getPeerIds());
    }
}