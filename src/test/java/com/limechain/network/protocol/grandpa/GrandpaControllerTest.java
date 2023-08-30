package com.limechain.network.protocol.grandpa;

import io.libp2p.core.PeerId;
import io.libp2p.core.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GrandpaControllerTest {
    private GrandpaController grandpaController;
    private final Stream stream = mock(Stream.class);
    private final PeerId peerId = mock(PeerId.class);
    private final GrandpaEngine engine = mock(GrandpaEngine.class);

    @BeforeEach
    void setup() {
        when(stream.remotePeerId()).thenReturn(peerId);
        grandpaController = new GrandpaController(stream);
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