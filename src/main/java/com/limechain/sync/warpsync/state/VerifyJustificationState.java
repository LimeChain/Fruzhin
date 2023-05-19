package com.limechain.sync.warpsync.state;

import com.limechain.network.protocol.warp.dto.WarpSyncFragment;
import com.limechain.sync.warpsync.WarpSyncMachine;
import lombok.extern.java.Log;

import java.util.logging.Level;

// VerifyJustificationState is going to be instantiated a lot of times, maybe we can make it a singleton in order to reduce performance overhead?
@Log
public class VerifyJustificationState implements WarpSyncState {
    private Exception error;

    @Override
    public void next(WarpSyncMachine sync) {
        if (this.error != null) {
            // Not sure what state we should transition to here.
            sync.setState(new FinishedState());
            return;
        }

        if (!sync.getFragmentsQueue().isEmpty()) {
            sync.setState(new VerifyJustificationState());
        } else if (sync.isFinished()) {
            sync.setState(new RuntimeDownloadState());
        } else {
            sync.setState(new RequestFragmentsState(sync.getLastFinalizedBlockHash()));
        }
    }

    @Override
    public void handle(WarpSyncMachine sync) {
        try {
            WarpSyncFragment fragment = sync.getFragmentsQueue().poll();
            log.log(Level.INFO, "Verifying justification...");
            // TODO: Throw error if not verified!
            boolean verified = fragment.getJustification().verify(sync.getAuthoritySet(), sync.getSetId());
            if (!verified) {
                throw new Exception("Justification could not be verified.");
            }
            // Set the latest finalized header
            // TODO: Persist header to DB?
            sync.setLastFinalizedBlockHash(fragment.getJustification().targetHash);
            log.log(Level.INFO, "Verified justification. Bloch hash is now at: " + sync.getLastFinalizedBlockHash().toString());
        } catch (Exception e) {
            log.log(Level.WARNING, "Error while verifying justification: " + e.getMessage());
            this.error = e;
        }
    }
}
