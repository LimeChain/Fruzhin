package com.limechain.sync.warpsync.state;

import com.limechain.sync.warpsync.SyncedState;
import com.limechain.sync.warpsync.WarpSyncMachine;
import com.limechain.exception.global.RuntimeCodeException;
import io.emeraldpay.polkaj.types.Hash256;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RuntimeDownloadStateTest {
    @InjectMocks
    private RuntimeDownloadState runtimeDownloadState;
    @Mock
    private SyncedState syncedState;
    @Mock
    private WarpSyncMachine warpSyncMachine;

    @Test
    void nextWhenNoErrorShouldSetRuntimeBuildState() {
        ReflectionTestUtils.setField(runtimeDownloadState, "error", null);

        runtimeDownloadState.next(warpSyncMachine);

        verify(warpSyncMachine).setWarpSyncState(any(RuntimeBuildState.class));
    }

    @Test
    void nextWhenErrorShouldSetRequestFragmentState() {
        ReflectionTestUtils.setField(runtimeDownloadState, "error", mock(Exception.class));
        Hash256 blockHash = mock(Hash256.class);
        when(syncedState.getLastFinalizedBlockHash()).thenReturn(blockHash);

        List<Object> capturedArguments = new ArrayList<>();
        try (MockedConstruction<RequestFragmentsState> stateMock = mockConstruction(RequestFragmentsState.class,
                (mock, context) -> capturedArguments.add(context.arguments().get(0)))) {
            runtimeDownloadState.next(warpSyncMachine);

            assertEquals(blockHash, capturedArguments.get(0));
            assertEquals(1, stateMock.constructed().size());
            RequestFragmentsState constructedState = stateMock.constructed().get(0);

            verify(warpSyncMachine).setWarpSyncState(constructedState);
        }
    }

    @Test
    void handleShouldTryToLoadSavedRuntime() throws RuntimeCodeException {
        runtimeDownloadState.handle(warpSyncMachine);

        verify(syncedState).loadSavedRuntimeCode();
    }

    @Test
    void handleWhenLoadFailsShouldTryToUpdateRuntime() throws RuntimeCodeException {
        doThrow(mock(RuntimeCodeException.class)).when(syncedState).loadSavedRuntimeCode();

        runtimeDownloadState.handle(warpSyncMachine);

        verify(syncedState).updateRuntimeCode();
    }

    @Test
    void handleUpdateRuntimeFailsShouldUpdateErrorField() throws RuntimeCodeException {
        doThrow(mock(RuntimeCodeException.class)).when(syncedState).loadSavedRuntimeCode();
        RuntimeCodeException updateRuntimeCodeException = mock(RuntimeCodeException.class);
        doThrow(updateRuntimeCodeException).when(syncedState).updateRuntimeCode();

        runtimeDownloadState.handle(warpSyncMachine);

        Exception error = (Exception) ReflectionTestUtils.getField(runtimeDownloadState, "error");
        assertEquals(updateRuntimeCodeException, error);
    }
}