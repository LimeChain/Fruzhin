package com.limechain.sync.warpsync.state;

import com.limechain.sync.warpsync.RuntimeBuilder;
import com.limechain.sync.warpsync.WarpSyncMachine;
import lombok.extern.java.Log;
import com.limechain.sync.warpsync.Runtime;
import java.util.logging.Level;

/**
 * Performs some runtime calls in order to obtain the current consensus-related parameters
 * of the chain. This might require obtaining some storage items, in which case they will also
 * be downloaded from a source in the Chain Information Download State
 */
@Log
public class RuntimeBuildState implements WarpSyncState {
    @Override
    public void next(WarpSyncMachine sync) {
        log.log(Level.INFO, "Done with runtime build");
        sync.setState(new ChainInformationDownloadState());
    }

    @Override
    public void handle(WarpSyncMachine sync) {
        // TODO: After runtime is downloaded, we are downloading and computing the information of the chain
        try {
            Runtime runtime = RuntimeBuilder.buildRuntime(sync.getRuntime());

            //Maybe don't need to close the instance
            runtime.getInstance().close();
        } catch (UnsatisfiedLinkError e) {
            log.log(Level.SEVERE, "Error loading wasm module");
            log.log(Level.SEVERE, e.getMessage(), e.getStackTrace());
        }
    }
}
