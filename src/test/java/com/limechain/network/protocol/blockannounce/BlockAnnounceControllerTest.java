package com.limechain.network.protocol.blockannounce;

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
class BlockAnnounceControllerTest {
    @InjectMocks
    private BlockAnnounceController blockAnnounceController;
    @Mock
    private Stream stream;
    @Mock
    private PeerId peerId;
    @Mock
    private BlockAnnounceEngine engine;

    @BeforeEach
    void setup() {
        blockAnnounceController.engine = engine;
    }

    @Test
    void sendHanshake() {
        when(stream.remotePeerId()).thenReturn(peerId);
        blockAnnounceController.sendHandshake();
        verify(engine).writeHandshakeToStream(stream, peerId);
    }

}