package com.limechain.sync.warpsync.state;

import com.limechain.sync.warpsync.WarpSyncMachine;
import lombok.extern.java.Log;

import java.util.logging.Level;

@Log
public class FinishedState implements WarpSyncState {

    public FinishedState() {
        log.log(Level.INFO, "Finished with warp sync!");
    }

    @Override
    public void next(WarpSyncMachine sync) {
        log.log(Level.INFO, "Finished! Finished with warp sync! Nothing to execute.");
    }

    @Override
    public void handle(WarpSyncMachine sync) {
        log.log(Level.INFO, "Finished with warp sync! Nothing to execute.");
    }
}
