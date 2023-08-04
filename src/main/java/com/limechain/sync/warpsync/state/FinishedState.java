package com.limechain.sync.warpsync.state;

import com.limechain.sync.warpsync.SyncedState;
import com.limechain.sync.warpsync.WarpSyncMachine;
import lombok.extern.java.Log;

import java.util.logging.Level;

@Log
public class FinishedState implements WarpSyncState {
    private final SyncedState syncedState = SyncedState.getInstance();
    public FinishedState() {
        log.log(Level.INFO, "Finished with warp sync!");
    }

    @Override
    public void next(WarpSyncMachine sync) {
        syncedState.getRuntime().getInstance().close();
        log.log(Level.INFO, "Closed sync runtime instance.");
        log.log(Level.INFO, "Finished! Finished with warp sync! Nothing to execute.");
    }

    @Override
    public void handle(WarpSyncMachine sync) {
        log.log(Level.INFO, "Finished with warp sync! Nothing to execute.");
    }
}
