package com.limechain.sync.warpsync.state;

import com.limechain.sync.warpsync.WarpSyncMachine;

/**
 * Performs some runtime calls in order to obtain the current consensus-related parameters
 * of the chain. This might require obtaining some storage items, in which case they must also
 * be downloaded from a source
 */
public class ChainInformationDownloadState implements WarpSyncState {
    @Override
    public void next(WarpSyncMachine sync) {
        // We're done with the warp sync process!
        sync.setWarpSyncState(new FinishedState());
    }

    @Override
    public void handle(WarpSyncMachine sync) {
        // TODO: After runtime is downloaded, we are downloading and computing the information of the chain
        // This information is retrieved using remoteCallRequests
    }
}
