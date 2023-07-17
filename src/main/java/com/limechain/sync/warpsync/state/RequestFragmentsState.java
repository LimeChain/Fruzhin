package com.limechain.sync.warpsync.state;

import com.limechain.network.protocol.warp.dto.WarpSyncResponse;
import com.limechain.sync.warpsync.WarpSyncMachine;
import io.emeraldpay.polkaj.types.Hash256;
import lombok.extern.java.Log;

import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;

@Log
public class RequestFragmentsState implements WarpSyncState {
    private final Hash256 blockHash;
    private WarpSyncResponse result;
    private Exception error;

    public RequestFragmentsState(Hash256 blockHash) {
        this.blockHash = blockHash;
    }

    @Override
    public void next(WarpSyncMachine sync) {
        if (this.error != null) {
            // Try again?
            sync.setWarpSyncState(new FinishedState());
            return;
        }
        if (this.result != null) {
            sync.setWarpSyncState(new VerifyJustificationState());
            return;
        }
        log.log(Level.WARNING, "RequestFragmentsState.next() called without result or error set.");
    }

    @Override
    public void handle(WarpSyncMachine sync) {
        try {
            log.log(Level.INFO, "Requesting fragments...");
            WarpSyncResponse resp = sync.getNetworkService().makeWarpSyncRequest(blockHash.toString());
            if (resp == null) {
                throw new Exception("No response received.");
            }
            log.log(Level.INFO, "Got response from warp sync: " + resp);

            if (resp.getFragments().length == 0) {
                log.log(Level.WARNING, "No fragments received.");
                return;
            }
            sync.setFinished(resp.isFinished());
            sync.setFragmentsQueue(new LinkedBlockingQueue<>(
                    Arrays.stream(resp.getFragments()).collect(java.util.stream.Collectors.toList()))
            );

            this.result = resp;
        } catch (Exception e) {
            // TODO: Set error state, next() will use to transition to correct next state.
            // This error state could be either recoverable or irrecoverable.
            log.log(Level.WARNING, "Error while requesting fragments: " + e.getMessage());
            this.error = e;
        }
    }
}
