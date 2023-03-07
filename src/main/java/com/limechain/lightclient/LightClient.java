package com.limechain.lightclient;

import com.limechain.rpc.http.server.HttpRpc;
import com.limechain.rpc.ws.server.WebSocketRPC;

import java.util.logging.Level;
import java.util.logging.Logger;

public class LightClient {
    // TODO: Add service dependencies i.e rpc, sync, network, etc.
    private static final Logger LOGGER = Logger.getLogger(LightClient.class.getName());
    private final HttpRpc httpRpc;
    private final WebSocketRPC wsRpc;
    private final String[] cliArgs;

    public LightClient (String[] cliArgs, HttpRpc httpRpc, WebSocketRPC wsRpc) {
        this.cliArgs = cliArgs;
        this.httpRpc = httpRpc;
        this.wsRpc = wsRpc;
    }

    public void start () {
        // TODO: Add business logic
        this.httpRpc.start(cliArgs);
        this.wsRpc.start(cliArgs);
        LOGGER.log(Level.INFO, "\uD83D\uDE80Started light client!");

        // We can access Spring Beans using the RPC Context!
        // LOGGER.log(Level.INFO, RPCContext.getBean(SystemRPCImpl.class).system_name());
    }

    public void stop () {
        // TODO: Stop running services
        this.httpRpc.stop();
        this.wsRpc.stop();
        LOGGER.log(Level.INFO, "\uD83D\uDED1Stopped light client!");
    }
}
