package com.limechain.sync.warpsync.action;

import com.limechain.rpc.server.AppBean;
import com.limechain.sync.warpsync.WarpSyncMachine;
import com.limechain.sync.warpsync.WarpSyncState;
import lombok.AllArgsConstructor;
import lombok.extern.java.Log;

import java.util.logging.Level;

/**
 * Creates a runtime instance using the downloaded code
 */
@Log
@AllArgsConstructor
public class RuntimeBuildAction implements WarpSyncAction {

    private final WarpSyncState warpSyncState;

    public RuntimeBuildAction() {
        this(AppBean.getBean(WarpSyncState.class));
    }

    @Override
    public void next(WarpSyncMachine sync) {
        log.log(Level.INFO, "Done with runtime build");
        //After runtime instance is built, we are building the information of the chain
        sync.setWarpSyncAction(new ChainInformationBuildAction());
    }

    @Override
    public void handle(WarpSyncMachine sync) {
    }
}
