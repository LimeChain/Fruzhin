package com.limechain.sync.warpsync.state;

import com.limechain.sync.warpsync.WarpSyncMachine;

public interface WarpSyncState {
    void next(WarpSyncMachine sync);

    void handle(WarpSyncMachine sync);
}
