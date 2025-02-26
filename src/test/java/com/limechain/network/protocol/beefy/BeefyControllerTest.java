package com.limechain.network.protocol.beefy;

import com.limechain.network.protocol.BaseUtils;
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
class BeefyControllerTest {

    @InjectMocks
    private BeefyController beefyController;
    @Mock
    private Stream stream;
    @Mock
    private PeerId peerId;
    @Mock
    private BeefyEngine engine;

    @BeforeEach
    void setup() throws NoSuchFieldException, IllegalAccessException {
        BaseUtils.setProtectedEngineField(beefyController, engine);
    }

    @Test
    void sendHandshake() {
        when(stream.remotePeerId()).thenReturn(peerId);
        beefyController.sendHandshake();
        verify(engine).writeHandshakeToStream(stream, peerId);
    }
}
