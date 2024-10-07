package com.limechain.client;

import com.limechain.network.Network;
import com.limechain.rpc.server.AppBean;
import com.limechain.sync.warpsync.WarpSyncMachine;
import lombok.SneakyThrows;
import lombok.extern.java.Log;

import java.util.logging.Level;

/**
 * Main light client class that starts and stops execution of
 * the client and hold references to dependencies
 */
@Log
public class LightClient implements HostNode {
    // TODO: Add service dependencies i.e rpc, sync, network, etc.
    // TODO: Do we need those as fields here...?
    private final Network network;
    private WarpSyncMachine warpSyncMachine;

    /**
     * @implNote the RpcApp is assumed to have been started before constructing the client,
     * as it relies on the application context
     */
    public LightClient() {
        this.network = AppBean.getBean(Network.class);
    }

    /**
     * Starts the light client by instantiating all dependencies and services
     */
    @SneakyThrows
    public void start() {
        this.network.start();

        while (true) {
            if (network.getKademliaService().getBootNodePeerIds().size() > 0) {
                if (this.network.getKademliaService().getSuccessfulBootNodes() > 0) {
                    log.log(Level.INFO, "Node successfully connected to a peer! Sync can start!");
                    this.warpSyncMachine = AppBean.getBean(WarpSyncMachine.class);
                    this.warpSyncMachine.start();
                    break;
                } else {
                    this.network.updateCurrentSelectedPeer();
                }
            }
            log.log(Level.INFO, "Waiting for peer connection...");
            Thread.sleep(10000);
        }
    }
}
