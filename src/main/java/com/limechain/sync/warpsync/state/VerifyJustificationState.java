package com.limechain.sync.warpsync.state;

import com.limechain.network.protocol.warp.dto.WarpSyncFragment;
import com.limechain.sync.JustificationVerifier;
import com.limechain.sync.warpsync.SyncedState;
import com.limechain.sync.warpsync.WarpSyncMachine;
import lombok.extern.java.Log;

import java.util.logging.Level;

// VerifyJustificationState is going to be instantiated a lot of times
// Maybe we can make it a singleton in order to reduce performance overhead?
@Log
public class VerifyJustificationState implements WarpSyncState {
    private final SyncedState syncedState = SyncedState.getInstance();
    private Exception error;

    @Override
    public void next(WarpSyncMachine sync) {
        if (this.error != null) {
            // Not sure what state we should transition to here.
            sync.setWarpSyncState(new FinishedState());
            return;
        }

        if (!sync.getFragmentsQueue().isEmpty()) {
            sync.setWarpSyncState(new VerifyJustificationState());
        } else if (syncedState.isWarpSyncFragmentsFinished()) {
            sync.setWarpSyncState(new RuntimeDownloadState());
        } else {
            sync.setWarpSyncState(new RequestFragmentsState(syncedState.getLastFinalizedBlockHash()));
        }
    }

    @Override
    public void handle(WarpSyncMachine sync) {
        try {
            syncedState.handleScheduledEvents();

            WarpSyncFragment fragment = sync.getFragmentsQueue().poll();
            log.log(Level.INFO, "Verifying justification...");
            if (fragment == null) {
                throw new RuntimeException("No such fragment");
            }
            boolean verified = JustificationVerifier.verify(
                    fragment.getJustification().precommits,
                    fragment.getJustification().round);
            if (!verified) {
                throw new RuntimeException("Justification could not be verified.");
            }

            // Set the latest finalized header and number
            // TODO: Persist header to DB?
            syncedState.setStateRoot(fragment.getHeader().getStateRoot());
            syncedState.setLastFinalizedBlockHash(fragment.getJustification().targetHash);
            syncedState.setLastFinalizedBlockNumber(fragment.getJustification().targetBlock);

            try {
                syncedState.handleAuthorityChanges(
                        fragment.getHeader().getDigest(),
                        fragment.getJustification().targetBlock);
                log.log(Level.INFO, "Verified justification. Block hash is now at #"
                        + syncedState.getLastFinalizedBlockNumber() + ": "
                        + syncedState.getLastFinalizedBlockHash().toString()
                        + " with state root " + syncedState.getStateRoot());
            } catch (Exception error) {
                this.error = error;
            }
        } catch (Exception e) {
            log.log(Level.WARNING, "Error while verifying justification: " + e.getMessage());
            this.error = e;
        }
    }
}
