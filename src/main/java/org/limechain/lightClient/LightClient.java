package org.limechain.lightClient;

import org.limechain.chain.ChainService;
import org.limechain.rpc.RPC;

public class LightClient {
    // TODO: Add service dependencies i.e rpc, sync, network, etc.
    private final ChainService chainService;
    private final RPC rpc;

    public LightClient (ChainService chainService, RPC rpc) {
        this.chainService = chainService;
        this.rpc = rpc;
    }

    public void start () {
        // TODO: Add business logic
        this.rpc.start();
        System.out.println("\uD83D\uDE80Started light client!");
    }

    public void stop () {
        // TODO: Stop running services
        this.rpc.stop();
        System.out.println("\uD83D\uDED1Stopped light client!");
    }
}
