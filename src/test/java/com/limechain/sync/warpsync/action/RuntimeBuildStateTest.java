package com.limechain.sync.warpsync.action;

import com.limechain.sync.warpsync.WarpSyncMachine;
import com.limechain.sync.warpsync.WarpSyncState;
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
    private RuntimeBuildAction runtimeBuildState;
    @Mock
    private WarpSyncState warpSyncState;
    @Mock
    private WarpSyncMachine warpSyncMachine;

    @Test
    void nextSetsChainInformationBuildState() {
        runtimeBuildState.next(warpSyncMachine);

        verify(warpSyncMachine).setWarpSyncAction(any(ChainInformationBuildAction.class));
    }

    @Test
    void handleCallsSyncStateBuildRuntime() {
        runtimeBuildState.handle(warpSyncMachine);

        verify(warpSyncState).buildRuntime();
    }

}