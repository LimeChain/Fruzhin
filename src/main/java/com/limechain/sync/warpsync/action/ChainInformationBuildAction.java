package com.limechain.sync.warpsync.action;

import com.limechain.rpc.server.AppBean;
import com.limechain.runtime.version.ApiVersion;
import com.limechain.sync.warpsync.WarpSyncMachine;
import com.limechain.sync.warpsync.WarpSyncState;
import com.limechain.utils.HashUtils;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.java.Log;

import java.util.logging.Level;

/**
 * Sets consensus protocol versions
 */
@Log
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class ChainInformationBuildAction implements WarpSyncAction {
    private final WarpSyncState warpSyncState;

    public ChainInformationBuildAction() {
        warpSyncState = AppBean.getBean(WarpSyncState.class);
    }

    @Override
    public void next(WarpSyncMachine sync) {
        log.log(Level.INFO, "Done with runtime build");
        sync.setWarpSyncAction(new ChainInformationDownloadAction());
    }

    @Override
    public void handle(WarpSyncMachine sync) {
        byte[][] hashedApiVersions = new byte[][]{
                HashUtils.hashWithBlake2bToLength("AuraApi".getBytes(), ApiVersion.NAME_HASH_LENGTH),
                HashUtils.hashWithBlake2bToLength("BabeApi".getBytes(), ApiVersion.NAME_HASH_LENGTH),
                HashUtils.hashWithBlake2bToLength("GrandpaApi".getBytes(), ApiVersion.NAME_HASH_LENGTH)
        };
        sync.getChainInformation().setRuntimeAuraVersion(
                warpSyncState.getRuntime().getVersion().getApis().getApiVersion(hashedApiVersions[0]));
        sync.getChainInformation().setRuntimeBabeVersion(
                warpSyncState.getRuntime().getVersion().getApis().getApiVersion(hashedApiVersions[1]));
        sync.getChainInformation().setRuntimeGrandpaVersion(
                warpSyncState.getRuntime().getVersion().getApis().getApiVersion(hashedApiVersions[2]));
        log.log(Level.INFO, "Aura Api version: " + sync.getChainInformation().getRuntimeAuraVersion()
                            + " Babe api version: " + sync.getChainInformation().getRuntimeBabeVersion() +
                            " Grandpa Api Version: " + sync.getChainInformation().getRuntimeGrandpaVersion());
        log.log(Level.INFO, "Runtime supports aura: " + sync.getChainInformation().runtimeHasAura());
        log.log(Level.INFO, "Runtime babe api is v1: " + sync.getChainInformation().runtimeBabeApiIsV1());
        log.log(Level.INFO, "Runtime grandpa supports current setId: "
                            + sync.getChainInformation().runtimeGrandpaSupportsCurrentSetId());
    }
}
