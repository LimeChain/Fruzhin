package com.limechain.lightclient;

import com.limechain.network.Network;
import com.limechain.rpc.server.AppBean;
import com.limechain.sync.warpsync.WarpSyncMachine;
import lombok.SneakyThrows;
import lombok.extern.java.Log;

import java.util.logging.Level;

@Log
public class FullClient implements NodeClient {
    /**
     * Starts the light client by instantiating all dependencies and services
     *
     * @implNote the RpcApp is assumed to have been started before starting the client,
     * as it relies on the application context
     */
    @SneakyThrows
    public void start() {
        // TODO: Initialize storage here (temporally)

        final Network network = AppBean.getBean(Network.class);
        network.start();

        while (true) {
            if (!network.kademliaService.getBootNodePeerIds().isEmpty()) {
                if (network.kademliaService.getSuccessfulBootNodes() > 0) {
                    break;
                }
                network.updateCurrentSelectedPeer();
            }

            log.log(Level.INFO, "Waiting for peer connection...");
            Thread.sleep(10000); // TODO: Maybe extract this number into an application.property as it's duplicated
        }

        log.log(Level.INFO, "Node successfully connected to a peer! Sync can start!");
        AppBean.getBean(WarpSyncMachine.class).start();
    }
}
