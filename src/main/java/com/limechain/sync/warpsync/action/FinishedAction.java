package com.limechain.sync.warpsync.action;

import com.limechain.rpc.server.AppBean;
import com.limechain.sync.warpsync.WarpSyncMachine;
import com.limechain.sync.warpsync.WarpSyncState;
import lombok.extern.java.Log;

import java.util.logging.Level;

@Log
public class FinishedAction implements WarpSyncAction {
    private final WarpSyncState warpSyncState;
    public FinishedAction() {
        this.warpSyncState = AppBean.getBean(WarpSyncState.class);
        log.log(Level.INFO, "Finished with warp sync!");
    }

    @Override
    public void next(WarpSyncMachine sync) {
        log.log(Level.INFO, "Closed sync runtime instance.");
        log.log(Level.INFO, "Finished! Finished with warp sync! Nothing to execute.");
    }

    @Override
    public void handle(WarpSyncMachine sync) {
        log.log(Level.INFO, "Finished with warp sync! Nothing to execute.");
    }
}
