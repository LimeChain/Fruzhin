package org.limechain.lightClient;

import org.limechain.chain.ChainService;

public class LightClient {
    // TODO: Add service dependencies i.e rpc, sync, network, etc.
    private ChainService chainService;

    public LightClient(ChainService chainService) {
        this.chainService = chainService;
    }
    public void start() {
        // Add business logic here
        System.out.println("\uD83D\uDE80Started light client!");
    }
}
