package com.limechain.sync.warpsync.state;

import com.limechain.sync.warpsync.SyncedState;
import com.limechain.sync.warpsync.WarpSyncMachine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RuntimeBuildStateTest {
    @InjectMocks
    private RuntimeBuildState runtimeBuildState;
    @Mock
    private SyncedState syncedState;
    @Mock
    private WarpSyncMachine warpSyncMachine;

    @Test
    void nextSetsChainInformationBuildState() {
        runtimeBuildState.next(warpSyncMachine);

        verify(warpSyncMachine).setWarpSyncState(any(ChainInformationBuildState.class));
    }

    @Test
    void handleCallsSyncStateBuildRuntime() {
        runtimeBuildState.handle(warpSyncMachine);

        verify(syncedState).buildRuntime(null);
    }

}