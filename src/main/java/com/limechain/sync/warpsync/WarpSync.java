package com.limechain.sync.warpsync;

import com.limechain.chain.ChainService;
import com.limechain.chain.lightsyncstate.LightSyncState;
import com.limechain.network.Network;
import com.limechain.network.protocol.warp.dto.WarpSyncResponse;
import lombok.extern.java.Log;

import java.util.logging.Level;

@Log
public class WarpSync {
    private static WarpSync warpSync;
    private final ChainService chainService;
    private final Network networkService;
    private WarpSyncStatus warpSyncStatus = WarpSyncStatus.NotStarted;

    public WarpSync(Network network, ChainService chainService) {
        this.networkService = network;
        this.chainService = chainService;
    }

    /**
     * Initializes singleton Network instance
     * This is used two times on startup
     *
     * @return Network instance saved in class or if not found returns new Network instance
     */
    public static WarpSync initialize(Network network, ChainService chainService) {
        if (warpSync != null) {
            log.log(Level.WARNING, "Network module already initialized.");
            return warpSync;
        }
        warpSync = new WarpSync(network, chainService);
        log.log(Level.INFO, "Initialized network module!");
        return warpSync;
    }

    private void makeSyncProgress() {
        try {
            switch (this.warpSyncStatus) {
                case NotStarted, InProgress -> {
                    log.log(Level.INFO, "Making sync progress...");
                    this.warpSyncStatus = WarpSyncStatus.InProgress;
                    LightSyncState initState = LightSyncState.decode(this.chainService.getGenesis().getLightSyncState());

                    WarpSyncResponse resp = this.networkService.makeWarpSyncRequest(initState.getFinalizedBlockHeader().getParentHash().toString());
                    log.log(Level.INFO, "Got response from warp sync: " + resp.toString());
                }
                case Completed -> log.log(Level.INFO, "Warp sync already finished.");
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "Error while making sync progress: " + e.getMessage());
        } finally {
            Thread t = new Thread(this::makeSyncProgress);
            t.start();
        }
    }

    public boolean isSyncing() {
        return warpSyncStatus != WarpSyncStatus.Completed;
    }

    public void start() {
        Thread t = new Thread(this::makeSyncProgress);
        t.start();
    }

    public void stop() {
    }
}
