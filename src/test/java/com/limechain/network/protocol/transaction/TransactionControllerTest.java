package com.limechain.network.protocol.transaction;

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
class TransactionControllerTest {

    @InjectMocks
    private TransactionController transactionController;
    @Mock
    private Stream stream;
    @Mock
    private PeerId peerId;
    @Mock
    private TransactionEngine engine;

    @BeforeEach
    void setup() throws NoSuchFieldException, IllegalAccessException {
        BaseUtils.setProtectedEngineField(transactionController, engine);
    }

    @Test
    void sendHandshake() {
        when(stream.remotePeerId()).thenReturn(peerId);
        transactionController.sendHandshake();
        verify(engine).writeHandshakeToStream(stream, peerId);
    }

    @Test
    void sendTransactionsMessage() {
        byte[] encodedCommitMessage = {1, 0, 0, 0, 2, 0, 1, 1, 1, 1, 0, 0, 0, 1, 2, 0};
        transactionController.sendTransactionsMessage(encodedCommitMessage);
        verify(engine).writeTransactionsMessage(stream, encodedCommitMessage);
    }
}
