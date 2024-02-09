package com.limechain.sync.warpsync.state;

import com.limechain.runtime.version.ApiVersion;
import com.limechain.sync.warpsync.SyncedState;
import com.limechain.sync.warpsync.WarpSyncMachine;
import com.limechain.utils.HashUtils;
import lombok.extern.java.Log;

import java.util.logging.Level;

/**
 * Sets consensus protocol versions
 */
@Log
public class ChainInformationBuildState implements WarpSyncState {
    private final SyncedState syncedState = SyncedState.getInstance();

    @Override
    public void next(WarpSyncMachine sync) {
        log.log(Level.INFO, "Done with runtime build");
        sync.setWarpSyncState(new ChainInformationDownloadState());
    }

    @Override
    public void handle(WarpSyncMachine sync) {
        byte[][] hashedApiVersions = new byte[][]{
                HashUtils.hashWithBlake2bToLength("AuraApi".getBytes(), ApiVersion.NAME_HASH_LENGTH),
                HashUtils.hashWithBlake2bToLength("BabeApi".getBytes(), ApiVersion.NAME_HASH_LENGTH),
                HashUtils.hashWithBlake2bToLength("GrandpaApi".getBytes(), ApiVersion.NAME_HASH_LENGTH)
        };
        sync.getChainInformation().setRuntimeAuraVersion(
                syncedState.getRuntime().getVersion().getApis().getApiVersion(hashedApiVersions[0]));
        sync.getChainInformation().setRuntimeBabeVersion(
                syncedState.getRuntime().getVersion().getApis().getApiVersion(hashedApiVersions[1]));
        sync.getChainInformation().setRuntimeGrandpaVersion(
                syncedState.getRuntime().getVersion().getApis().getApiVersion(hashedApiVersions[2]));
        log.log(Level.INFO, "Aura Api version: " + sync.getChainInformation().getRuntimeAuraVersion()
                + " Babe api version: " + sync.getChainInformation().getRuntimeBabeVersion() +
                " Grandpa Api Version: " + sync.getChainInformation().getRuntimeGrandpaVersion());
        log.log(Level.INFO, "Runtime supports aura: " + sync.getChainInformation().runtimeHasAura());
        log.log(Level.INFO, "Runtime babe api is v1: " + sync.getChainInformation().runtimeBabeApiIsV1());
        log.log(Level.INFO, "Runtime grandpa supports current setId: "
                + sync.getChainInformation().runtimeGrandpaSupportsCurrentSetId());
    }
}
