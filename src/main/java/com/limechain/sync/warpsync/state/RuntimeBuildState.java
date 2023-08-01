package com.limechain.sync.warpsync.state;

import com.limechain.sync.warpsync.SyncedState;
import com.limechain.sync.warpsync.runtime.RuntimeBuilder;
import com.limechain.sync.warpsync.WarpSyncMachine;
import lombok.extern.java.Log;
import com.limechain.sync.warpsync.runtime.Runtime;

import java.util.logging.Level;

/**
    Creates a runtime instance using the downloaded code
 */
@Log
public class RuntimeBuildState implements WarpSyncState {
    private final SyncedState syncedState = SyncedState.getInstance();
    @Override
    public void next(WarpSyncMachine sync) {
        log.log(Level.INFO, "Done with runtime build");
        //After runtime instance is built, we are building the information of the chain
        sync.setWarpSyncState(new ChainInformationBuildState());
    }

    @Override
    public void handle(WarpSyncMachine sync) {
        try {
            Runtime runtime = RuntimeBuilder.buildRuntime(syncedState.getRuntimeCode());
            syncedState.setRuntime(runtime);
        } catch (UnsatisfiedLinkError e) {
            log.log(Level.SEVERE, "Error loading wasm module");
            log.log(Level.SEVERE, e.getMessage(), e.getStackTrace());
        }
    }
}
