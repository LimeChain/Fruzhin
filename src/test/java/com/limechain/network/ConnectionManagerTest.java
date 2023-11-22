package com.limechain.network;

import com.limechain.network.dto.PeerInfo;
import com.limechain.network.dto.ProtocolStreamType;
import com.limechain.network.dto.ProtocolStreams;
import com.limechain.network.protocol.blockannounce.scale.BlockAnnounceMessage;
import com.limechain.network.protocol.warp.dto.BlockHeader;
import io.emeraldpay.polkaj.types.Hash256;
import io.libp2p.core.PeerId;
import io.libp2p.core.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigInteger;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConnectionManagerTest {
    @InjectMocks
    private ConnectionManager connectionManager;
    @Mock
    private PeerId peerId;
    @Mock
    private PeerInfo peerInfo;

    // BLOCK ANNOUNCE TESTS
    @Test
    void addBlockAnnounceStreamShouldAddPeerIfNotPresent() {
        Stream stream = mock(Stream.class);
        when(stream.remotePeerId()).thenReturn(peerId);
        when(stream.isInitiator()).thenReturn(false);

        connectionManager.addBlockAnnounceStream(stream);

        assertTrue(connectionManager.peers.containsKey(peerId));
    }

    @Test
    void addBlockAnnounceResponderStreamShouldAddStreamToExistingPeer() {
        Stream stream = mock(Stream.class);
        when(stream.remotePeerId()).thenReturn(peerId);
        when(stream.isInitiator()).thenReturn(false);
        ProtocolStreams protocolStreams = mock(ProtocolStreams.class);
        when(peerInfo.getProtocolStreams(ProtocolStreamType.BLOCK_ANNOUNCE)).thenReturn(protocolStreams);
        connectionManager.peers.put(peerId, peerInfo);

        connectionManager.addBlockAnnounceStream(stream);

        verify(protocolStreams).setResponder(stream);
    }

    @Test
    void addBlockAnnounceInitiatorStreamShouldAddStreamToExistingPeer() {
        Stream stream = mock(Stream.class);
        when(stream.remotePeerId()).thenReturn(peerId);
        when(stream.isInitiator()).thenReturn(true);
        ProtocolStreams protocolStreams = mock(ProtocolStreams.class);
        when(peerInfo.getProtocolStreams(ProtocolStreamType.BLOCK_ANNOUNCE)).thenReturn(protocolStreams);
        connectionManager.peers.put(peerId, peerInfo);

        connectionManager.addBlockAnnounceStream(stream);

        verify(protocolStreams).setInitiator(stream);
    }

    @Test
    void addBlockAnnounceResponderStreamWhenAlreadyConnectedShouldCloseStream() {
        Stream stream = mock(Stream.class);
        when(stream.remotePeerId()).thenReturn(peerId);
        when(stream.isInitiator()).thenReturn(false);
        ProtocolStreams protocolStreams = mock(ProtocolStreams.class);
        when(protocolStreams.getResponder()).thenReturn(mock(Stream.class));
        when(peerInfo.getProtocolStreams(ProtocolStreamType.BLOCK_ANNOUNCE)).thenReturn(protocolStreams);
        connectionManager.peers.put(peerId, peerInfo);

        connectionManager.addBlockAnnounceStream(stream);

        verify(protocolStreams, never()).setResponder(any());
        verify(stream).close();
    }

    @Test
    void closeBlockAnnounceInitiatorStream() {
        Stream stream = mock(Stream.class);
        when(stream.remotePeerId()).thenReturn(peerId);
        when(stream.isInitiator()).thenReturn(true);
        ProtocolStreams protocolStreams = mock(ProtocolStreams.class);
        when(peerInfo.getProtocolStreams(ProtocolStreamType.BLOCK_ANNOUNCE)).thenReturn(protocolStreams);
        connectionManager.peers.put(peerId, peerInfo);

        connectionManager.closeBlockAnnounceStream(stream);

        verify(protocolStreams).setInitiator(null);
    }

    @Test
    void closeBlockAnnounceResponderStream() {
        Stream stream = mock(Stream.class);
        when(stream.remotePeerId()).thenReturn(peerId);
        when(stream.isInitiator()).thenReturn(false);
        ProtocolStreams protocolStreams = mock(ProtocolStreams.class);
        when(peerInfo.getProtocolStreams(ProtocolStreamType.BLOCK_ANNOUNCE)).thenReturn(protocolStreams);
        connectionManager.peers.put(peerId, peerInfo);

        connectionManager.closeBlockAnnounceStream(stream);

        verify(protocolStreams).setResponder(null);
    }

    // GRANDPA TESTS
    @Test
    void addGrandpaStreamShouldAddPeerIfNotPresent() {
        Stream stream = mock(Stream.class);
        when(stream.remotePeerId()).thenReturn(peerId);
        when(stream.isInitiator()).thenReturn(false);

        connectionManager.addGrandpaStream(stream);

        assertTrue(connectionManager.peers.containsKey(peerId));
    }

    @Test
    void addGrandpaResponderStreamShouldAddStreamToExistingPeer() {
        Stream stream = mock(Stream.class);
        when(stream.remotePeerId()).thenReturn(peerId);
        when(stream.isInitiator()).thenReturn(false);
        ProtocolStreams protocolStreams = mock(ProtocolStreams.class);
        when(peerInfo.getProtocolStreams(ProtocolStreamType.GRANDPA)).thenReturn(protocolStreams);
        connectionManager.peers.put(peerId, peerInfo);

        connectionManager.addGrandpaStream(stream);

        verify(protocolStreams).setResponder(stream);
    }

    @Test
    void addGrandpaInitiatorStreamShouldAddStreamToExistingPeer() {
        Stream stream = mock(Stream.class);
        when(stream.remotePeerId()).thenReturn(peerId);
        when(stream.isInitiator()).thenReturn(true);
        ProtocolStreams protocolStreams = mock(ProtocolStreams.class);
        when(peerInfo.getProtocolStreams(ProtocolStreamType.GRANDPA)).thenReturn(protocolStreams);
        connectionManager.peers.put(peerId, peerInfo);

        connectionManager.addGrandpaStream(stream);

        verify(protocolStreams).setInitiator(stream);
    }

    @Test
    void addGrandpaInitiatorStreamWhenAlreadyConnectedShouldCloseStream() {
        Stream stream = mock(Stream.class);
        when(stream.remotePeerId()).thenReturn(peerId);
        when(stream.isInitiator()).thenReturn(true);
        ProtocolStreams protocolStreams = mock(ProtocolStreams.class);
        when(protocolStreams.getInitiator()).thenReturn(mock(Stream.class));
        when(peerInfo.getProtocolStreams(ProtocolStreamType.GRANDPA)).thenReturn(protocolStreams);
        connectionManager.peers.put(peerId, peerInfo);

        connectionManager.addGrandpaStream(stream);

        verify(protocolStreams, never()).setInitiator(any());
        verify(stream).close();
    }

    @Test
    void addGrandpaResponderStreamWhenAlreadyConnectedShouldCloseStream() {
        Stream stream = mock(Stream.class);
        when(stream.remotePeerId()).thenReturn(peerId);
        when(stream.isInitiator()).thenReturn(false);
        ProtocolStreams protocolStreams = mock(ProtocolStreams.class);
        when(protocolStreams.getResponder()).thenReturn(mock(Stream.class));
        when(peerInfo.getProtocolStreams(ProtocolStreamType.GRANDPA)).thenReturn(protocolStreams);
        connectionManager.peers.put(peerId, peerInfo);

        connectionManager.addGrandpaStream(stream);

        verify(protocolStreams, never()).setResponder(any());
        verify(stream).close();
    }

    @Test
    void closeGrandpaInitiatorStream() {
        Stream stream = mock(Stream.class);
        when(stream.remotePeerId()).thenReturn(peerId);
        when(stream.isInitiator()).thenReturn(true);
        ProtocolStreams protocolStreams = mock(ProtocolStreams.class);
        when(peerInfo.getProtocolStreams(ProtocolStreamType.GRANDPA)).thenReturn(protocolStreams);
        connectionManager.peers.put(peerId, peerInfo);

        connectionManager.closeGrandpaStream(stream);

        verify(protocolStreams).setInitiator(null);
    }

    @Test
    void closeGrandpaResponderStream() {
        Stream stream = mock(Stream.class);
        when(stream.remotePeerId()).thenReturn(peerId);
        when(stream.isInitiator()).thenReturn(false);
        ProtocolStreams protocolStreams = mock(ProtocolStreams.class);
        when(peerInfo.getProtocolStreams(ProtocolStreamType.GRANDPA)).thenReturn(protocolStreams);
        connectionManager.peers.put(peerId, peerInfo);

        connectionManager.closeGrandpaStream(stream);

        verify(protocolStreams).setResponder(null);
    }

    // OTHER
    @Test
    void getPeerInfo() {
        connectionManager.peers.put(peerId, peerInfo);
        assertSame(peerInfo, connectionManager.getPeerInfo(peerId));
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
        verify(peerInfo, never()).setBestBlock(any());
        verify(peerInfo, never()).setBestBlockHash(any());
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
        Arrays.fill(hash, (byte) 3 );
        when(header.getHash()).thenReturn(new Hash256(hash));

        connectionManager.updatePeer(peerId, message);

        verify(peerInfo).setBestBlock(BigInteger.TEN);
        verify(peerInfo).setBestBlockHash(new Hash256(hash));
    }

    @Test
    void isBlockAnnounceConnected() {
        connectionManager.peers.put(peerId, peerInfo);
        ProtocolStreams protocolStreams = mock(ProtocolStreams.class);
        when(peerInfo.getBlockAnnounceStreams()).thenReturn(protocolStreams);
        when(protocolStreams.getResponder()).thenReturn(mock(Stream.class));
        assertTrue(connectionManager.isBlockAnnounceConnected(peerId));
    }

    @Test
    void isGrandpaConnected() {
        connectionManager.peers.put(peerId, peerInfo);
        ProtocolStreams protocolStreams = mock(ProtocolStreams.class);
        when(peerInfo.getGrandpaStreams()).thenReturn(protocolStreams);
        when(protocolStreams.getResponder()).thenReturn(mock(Stream.class));
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