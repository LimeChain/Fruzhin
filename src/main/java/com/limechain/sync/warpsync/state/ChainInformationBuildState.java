package com.limechain.sync.warpsync.state;

import com.limechain.sync.warpsync.WarpSyncMachine;
import com.limechain.utils.HashUtils;
import lombok.extern.java.Log;

import java.math.BigInteger;
import java.util.logging.Level;

import static com.limechain.sync.warpsync.RuntimeApis.API_VERSION_LENGTH;

/**
 * Performs some runtime calls in order to obtain the current consensus-related parameters
 * of the chain. This might require obtaining some storage items, in which case they will also
 * be downloaded from a source in the Chain Information Download State
 */
@Log
public class ChainInformationBuildState implements WarpSyncState {
    @Override
    public void next(WarpSyncMachine sync) {
        log.log(Level.INFO, "Done with runtime build");
        sync.setState(new FinishedState());
    }

    @Override
    public void handle(WarpSyncMachine sync) {
        byte[][] hashedApiVersions = new byte[][]{
                HashUtils.hashWithBlake2bToLength("AuraApi".getBytes(), API_VERSION_LENGTH),
                HashUtils.hashWithBlake2bToLength("BabeApi".getBytes(), API_VERSION_LENGTH),
                HashUtils.hashWithBlake2bToLength("GrandpaApi".getBytes(), API_VERSION_LENGTH)
        };
        BigInteger auraApiVersion =
                sync.getRuntime().getVersion().getRuntimeApis().getApiVersion(hashedApiVersions[0]);
        BigInteger babeApiVersion =
                sync.getRuntime().getVersion().getRuntimeApis().getApiVersion(hashedApiVersions[1]);
        BigInteger GrandpaApiVersion =
                sync.getRuntime().getVersion().getRuntimeApis().getApiVersion(hashedApiVersions[2]);
        log.log(Level.INFO, "Aura Api version: " + auraApiVersion + " Babe api version: " + babeApiVersion +
                " Grandpa Api Version: " + GrandpaApiVersion);
    }
}
