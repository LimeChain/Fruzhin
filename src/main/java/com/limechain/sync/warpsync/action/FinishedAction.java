package com.limechain.sync.warpsync.action;

import com.limechain.rpc.server.AppBean;
import com.limechain.sync.warpsync.WarpSyncMachine;
import com.limechain.sync.warpsync.WarpSyncState;
import lombok.extern.java.Log;

@Log
public class FinishedAction implements WarpSyncAction {
    private final WarpSyncState warpSyncState;
    public FinishedAction() {
        this.warpSyncState = AppBean.getBean(WarpSyncState.class);
        log.info("Finished with warp sync!");
    }

    @Override
    public void next(WarpSyncMachine sync) {
        warpSyncState.getRuntime().close();
        log.info("Closed sync runtime instance.");
        log.info("Finished with warp sync! Nothing to execute.");
    }

    @Override
    public void handle(WarpSyncMachine sync) {
        log.info("Finished with warp sync! Nothing to execute.");
    }
}
