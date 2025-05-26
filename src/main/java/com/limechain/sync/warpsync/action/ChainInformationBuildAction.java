package com.limechain.sync.warpsync.action;

import com.limechain.rpc.server.AppBean;
import com.limechain.runtime.version.ApiVersionName;
import com.limechain.runtime.version.ApiVersions;
import com.limechain.sync.warpsync.WarpSyncMachine;
import com.limechain.sync.warpsync.WarpSyncState;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.java.Log;

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
        log.info("Done with runtime build");
        sync.setWarpSyncAction(new ChainInformationDownloadAction());
    }

    @Override
    public void handle(WarpSyncMachine sync) {
        ApiVersions versions = warpSyncState.getRuntime().getCachedVersion().getApis();
        sync.getChainInformation().setRuntimeAuraVersion(
                versions.getApiVersion(ApiVersionName.AURA_API.getHashedName()));
        sync.getChainInformation().setRuntimeBabeVersion(
                versions.getApiVersion(ApiVersionName.BABE_API.getHashedName()));
        sync.getChainInformation().setRuntimeGrandpaVersion(
                versions.getApiVersion(ApiVersionName.GRANDPA_API.getHashedName()));
        log.info(String.format("Aura Api version: %d Babe api version: %d Grandpa Api Version: %d ",
                sync.getChainInformation().getRuntimeAuraVersion(),
                sync.getChainInformation().getRuntimeBabeVersion(),
                sync.getChainInformation().getRuntimeGrandpaVersion()));
        log.info(String.format("Runtime supports aura: %b", sync.getChainInformation().runtimeHasAura()));
        log.info(String.format("Runtime babe api is v1: %b", sync.getChainInformation().runtimeBabeApiIsV1()));
        log.info(String.format("Runtime grandpa supports current setId: %b",
                sync.getChainInformation().runtimeGrandpaSupportsCurrentSetId()));
    }
}
