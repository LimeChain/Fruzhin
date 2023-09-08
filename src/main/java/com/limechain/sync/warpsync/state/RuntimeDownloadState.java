package com.limechain.sync.warpsync.state;

import com.limechain.sync.warpsync.SyncedState;
import com.limechain.sync.warpsync.WarpSyncMachine;
import com.limechain.sync.warpsync.dto.RuntimeCodeException;
import lombok.extern.java.Log;

import java.util.logging.Level;

@Log
public class RuntimeDownloadState implements WarpSyncState {
    private final SyncedState syncedState = SyncedState.getInstance();
    private Exception error;

    @Override
    public void next(WarpSyncMachine sync) {
        if (this.error != null) {
            sync.setWarpSyncState(new RequestFragmentsState(syncedState.getLastFinalizedBlockHash()));
            return;
        }
        // After runtime is downloaded, we have to build the runtime and then build chain information
        sync.setWarpSyncState(new RuntimeBuildState());
    }

    @Override
    public void handle(WarpSyncMachine sync) {
        try {
            log.log(Level.INFO, "Loading saved runtime...");
            syncedState.loadSavedRuntimeCode();
            return;
        } catch (RuntimeCodeException e) {
            this.error = e;
        }

        try {
            log.log(Level.INFO, "Downloading runtime...");
            syncedState.updateRuntimeCode();
        } catch (RuntimeCodeException e) {
            this.error = e;
        }
    }
}
