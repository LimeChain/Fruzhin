package com.limechain.network;

import io.libp2p.core.PeerId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class ConnectionManagerTest {
    ConnectionManager connectionManager;
    PeerId peerId = mock(PeerId.class);

    @BeforeEach
    void setUp() {
        connectionManager = new ConnectionManager();
    }

    @Test
    void addPeer() {
        connectionManager.addPeer(peerId);

        assertTrue(connectionManager.peers.contains(peerId));
    }

    @Test
    void removePeer() {
        connectionManager.peers.add(peerId);
        connectionManager.removePeer(peerId);

        assertFalse(connectionManager.peers.contains(peerId));
    }

    @Test
    void isConnected() {
        connectionManager.peers.add(peerId);

        assertTrue(connectionManager.isConnected(peerId));
    }

    @Test
    void getPeerIds() {
        assertSame(connectionManager.peers, connectionManager.getPeerIds());
    }
}