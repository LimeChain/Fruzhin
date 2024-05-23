package com.limechain.sync.warpsync.action;

import com.limechain.sync.warpsync.WarpSyncMachine;
import lombok.extern.java.Log;

import java.util.logging.Level;

/**
 * Performs some runtime calls in order to obtain the current consensus-related parameters
 * of the chain. This might require obtaining some storage items, in which case they will also
 * be downloaded from a source in the Chain Information Download State
 */
@Log
public class ChainInformationDownloadAction implements WarpSyncAction {
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
        sync.setWarpSyncAction(new FinishedAction());
    }

    @Override
    public void handle(WarpSyncMachine sync) {
        // TODO: After runtime is downloaded, we are downloading and computing the information of the chain
        // This information is retrieved using remoteCallRequests

        log.log(Level.INFO, "Downloading chain information...");
        Object[] responses = new Object[runtimeFunctionCalls.length];

        //Make a call for every runtime function we need
        for (int i = 0; i < runtimeFunctionCalls.length; i++) {
            try {
                //TODO Make runtime calls here
                //responses[i] = sync.getRuntime().call(runtimeFunctionCalls[i]);
                log.log(Level.INFO, "Made a runtime call \"" + runtimeFunctionCalls[i] + "\" : " + responses[i]);
            } catch (Exception e) {
                log.log(Level.WARNING, e.getMessage(), e.getStackTrace());
            }
        }
        log.log(Level.INFO, "Downloaded chain information");
    }
}
