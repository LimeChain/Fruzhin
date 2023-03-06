package org.limechain.lightClient;

import org.limechain.rpc.server.RPC;
import org.limechain.ws.server.WebSocketRPC;

public class LightClient {
    // TODO: Add service dependencies i.e rpc, sync, network, etc.
    private final RPC rpc;
    private final WebSocketRPC wsrpc;

    public LightClient (RPC rpc, WebSocketRPC wsrpc) {
        this.rpc = rpc;
        this.wsrpc = wsrpc;
    }

    public void start () {
        // TODO: Add business logic
        this.rpc.start();
        this.wsrpc.start();
        System.out.println("\uD83D\uDE80Started light client!");

        // We can access Spring Beans using the RPC Context!
        // System.out.println(RPCContext.getBean(SystemRPCImpl.class).system_name());
    }

    public void stop () {
        // TODO: Stop running services
        this.rpc.stop();
        this.wsrpc.stop();
        System.out.println("\uD83D\uDED1Stopped light client!");
    }
}
