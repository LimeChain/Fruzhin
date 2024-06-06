package com.limechain.sync.warpsync.action;

import com.limechain.sync.warpsync.WarpSyncMachine;

public interface WarpSyncAction {
    void next(WarpSyncMachine sync);

    void handle(WarpSyncMachine sync);
}
