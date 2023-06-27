package com.limechain.network;

import com.limechain.network.protocol.blockannounce.scale.BlockAnnounceHandshake;
import com.limechain.network.protocol.blockannounce.scale.BlockAnnounceMessage;
import com.limechain.network.protocol.warp.dto.BlockHeader;
import io.emeraldpay.polkaj.types.Hash256;
import io.libp2p.core.PeerId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ConnectionManagerTest {
    ConnectionManager connectionManager;
    PeerId peerId = mock(PeerId.class);
    BlockAnnounceHandshake blockAnnounceHandshake = mock(BlockAnnounceHandshake.class);

    @BeforeEach
    void setUp() {
        connectionManager = new ConnectionManager();
    }

    @Test
    void addPeer() {
        connectionManager.addPeer(peerId, blockAnnounceHandshake);

        assertSame(blockAnnounceHandshake, connectionManager.peers.get(peerId));
    }

    @Test
    void removePeer() {
        connectionManager.peers.put(peerId, blockAnnounceHandshake);
        connectionManager.removePeer(peerId);

        assertFalse(connectionManager.peers.containsKey(peerId));
    }

    @Test
    void updatePeerShouldDoNothingIfNotBestBlock() {
        connectionManager.peers.put(peerId, blockAnnounceHandshake);
        BlockAnnounceMessage message = mock(BlockAnnounceMessage.class);
        when(message.isBestBlock()).thenReturn(false);

        connectionManager.updatePeer(peerId, message);
        verifyNoInteractions(blockAnnounceHandshake);
    }

    @Test
    void updatePeerShouldUpdateWhenBestBlock() {
        connectionManager.peers.put(peerId, new BlockAnnounceHandshake());
        BlockAnnounceMessage message = mock(BlockAnnounceMessage.class);
        when(message.isBestBlock()).thenReturn(true);
        BlockHeader header = mock(BlockHeader.class);
        when(message.getHeader()).thenReturn(header);
        when(header.getBlockNumber()).thenReturn(BigInteger.TEN);
        byte[] hash = new byte[32];
        Arrays.fill( hash, (byte) 3 );
        when(header.hash()).thenReturn(hash);

        connectionManager.updatePeer(peerId, message);

        final var updatedPeerData = connectionManager.peers.get(peerId);
        assertEquals("10", updatedPeerData.getBestBlock());
        assertEquals(new Hash256(hash), updatedPeerData.getBestBlockHash());
    }

    @Test
    void isConnected() {
        connectionManager.peers.put(peerId, blockAnnounceHandshake);

        assertTrue(connectionManager.isConnected(peerId));
    }

    @Test
    void getPeerIds() {
        assertSame(connectionManager.peers.keySet(), connectionManager.getPeerIds());
    }
}