package com.limechain.lightclient;

import com.limechain.network.ConnectionManager;
import com.limechain.network.Network;
import com.limechain.rpc.server.AppBean;
import com.limechain.rpc.server.RpcApp;
import com.limechain.sync.warpsync.WarpSyncMachine;
import lombok.SneakyThrows;
import lombok.extern.java.Log;

import java.util.logging.Level;

/**
 * Main light client class that starts and stops execution of
 * the client and hold references to dependencies
 */
@Log
public class LightClient {
    // TODO: Add service dependencies i.e rpc, sync, network, etc.
    private final String[] cliArgs;
    private final RpcApp rpcApp;
    private final ConnectionManager connectionManager = ConnectionManager.getInstance();
    private Network network;
    private WarpSyncMachine warpSyncMachine;

    public LightClient(String[] cliArgs, RpcApp rpcApp) {
        this.cliArgs = cliArgs;
        this.rpcApp = rpcApp;
    }

    /**
     * Starts the light client by instantiating all dependencies and services
     */
    @SneakyThrows
    public void start() {
        // TODO: Add business logic
        this.rpcApp.start(cliArgs);

        this.network = AppBean.getBean(Network.class);
        this.network.start();

        while (true) {
            if (network.kademliaService.getBootNodePeerIds().size() > 0) {
                if (this.network.kademliaService.getSuccessfulBootNodes() > 0) {
                    log.log(Level.INFO, "Node successfully connected to a peer! Sync can start!");
                    this.warpSyncMachine = AppBean.getBean(WarpSyncMachine.class);
                    this.warpSyncMachine.start();
                    log.log(Level.INFO, "\uD83D\uDE80Started light client!");
                    break;
                } else {
                    this.network.updateCurrentSelectedPeer();
                }
            }
            log.log(Level.INFO, "Waiting for peer connection...");
            Thread.sleep(10000);
        }
    }

    /**
     * Stops the light client by shutting down all running services
     */
    public void stop() {
        // TODO: Stop running services
        this.warpSyncMachine.stop();
        this.network.stop();
        this.rpcApp.stop();
        log.log(Level.INFO, "\uD83D\uDED1Stopped light client!");
        doExit();
    }

    protected void doExit() {
        System.exit(0);
    }
}
