package com.limechain.sync.warpsync.state;

import com.limechain.sync.warpsync.SyncedState;
import com.limechain.sync.warpsync.WarpSyncMachine;
import lombok.extern.java.Log;

import java.util.logging.Level;

/**
 * Performs some runtime calls in order to obtain the current consensus-related parameters
 * of the chain. This might require obtaining some storage items, in which case they will also
 * be downloaded from a source in the Chain Information Download State
 */
@Log
public class ChainInformationDownloadState implements WarpSyncState {
    private final SyncedState syncedState = SyncedState.getInstance();
    private String[] runtimeFunctionCalls = new String[]{
            "AuraApi_slot_duration",
            "AuraApi_authorities",
            "BabeApi_current_epoch",
            "BabeApi_next_epoch",
            "BabeApi_configuration",
            "GrandpaApi_grandpa_authorities",
            "GrandpaApi_current_set_id"
    };

    @Override
    public void next(WarpSyncMachine sync) {
        // We're done with the warp sync process!
        sync.setWarpSyncState(new FinishedState());
    }

    @Override
    public void handle(WarpSyncMachine sync) {
        // TODO: After runtime is downloaded, we are downloading and computing the information of the chain
        // This information is retrieved using remoteCallRequests

        log.log(Level.INFO, "Downloading chain information...");
        Object[] responses = new Object[runtimeFunctionCalls.length];

        //Make a call for every runtime function we need
        try {
            //TODO Make runtime calls here
            if (sync.getChainInformation().runtimeHasAura()) {
                responses[0] = syncedState.getRuntime().call("AuraApi_slot_duration");
                responses[1] = syncedState.getRuntime().call("AuraApi_authorities");
            }
            if (!sync.getChainInformation().getRuntimeBabeVersion().equals(-1)) {
                responses[2] = syncedState.getRuntime().call("BabeApi_current_epoch");
                responses[3] = syncedState.getRuntime().call("BabeApi_next_epoch");
                responses[4] = syncedState.getRuntime().call("BabeApi_configuration");
            }
            if (!sync.getChainInformation().getRuntimeGrandpaVersion().equals(-1)) {
                responses[5] = syncedState.getRuntime().call("GrandpaApi_grandpa_authorities");
            }
            if (sync.getChainInformation().runtimeGrandpaSupportsCurrentSetId()) {
                responses[6] = syncedState.getRuntime().call("GrandpaApi_current_set_id");
            }
            for (int i = 0; i < 7; i++) {
                log.log(Level.INFO, " Runtime call " + i + " " + responses[i]);
            }
        } catch (Exception e) {
            log.log(Level.WARNING, e.getMessage(), e.getStackTrace());
        }
        log.log(Level.INFO, "Downloaded chain information");
    }
}
