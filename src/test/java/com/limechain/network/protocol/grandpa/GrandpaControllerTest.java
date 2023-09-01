package com.limechain.network.protocol.grandpa;

import io.libp2p.core.PeerId;
import io.libp2p.core.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GrandpaControllerTest {
    @InjectMocks
    private GrandpaController grandpaController;
    @Mock
    private Stream stream;
    @Mock
    private PeerId peerId;
    @Mock
    private GrandpaEngine engine;

    @BeforeEach
    void setup() {
        when(stream.remotePeerId()).thenReturn(peerId);
        grandpaController.engine = engine;
    }

    @Test
    void sendHandshake() {
        grandpaController.sendHandshake();
        verify(engine).writeHandshakeToStream(stream, peerId);
    }

    @Test
    void sendNeighbourMessage() {
        grandpaController.sendNeighbourMessage();
        verify(engine).writeNeighbourMessage(stream, peerId);
    }
}