package com.limechain.sync.fullsync;

public interface FullSyncState {
    FullSyncState transition(FullSyncMachine fullSyncMachine);
}
