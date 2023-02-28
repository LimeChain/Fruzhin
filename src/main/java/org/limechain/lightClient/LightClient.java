package org.limechain.lightClient;

import org.limechain.rpc.RPC;

public class LightClient {
    // TODO: Add service dependencies i.e rpc, sync, network, etc.
    private final RPC rpc;

    public LightClient (RPC rpc) {
        this.rpc = rpc;
    }

    public void start () {
        // TODO: Add business logic
        this.rpc.start();
        System.out.println("\uD83D\uDE80Started light client!");

        // We can access Spring Beans using the RPC Context!
        // System.out.println(RPCContext.getBean(SystemRPCImpl.class).system_name());
    }

    public void stop () {
        // TODO: Stop running services
        this.rpc.stop();
        System.out.println("\uD83D\uDED1Stopped light client!");
    }
}
