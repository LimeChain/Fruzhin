package com.limechain.lightclient;

import com.limechain.rpc.http.server.HttpRpc;
import com.limechain.rpc.ws.server.WebSocketRPC;
import lombok.extern.java.Log;

import java.util.logging.Level;

@Log
public class LightClient {
    // TODO: Add service dependencies i.e rpc, sync, network, etc.
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
        log.log(Level.INFO, "\uD83D\uDE80Started light client!");

        // We can access Spring Beans using the RPC Context!
        // log.log(Level.INFO, RPCContext.getBean(SystemRPCImpl.class).system_name());
    }

    public void stop () {
        // TODO: Stop running services
        this.httpRpc.stop();
        this.wsRpc.stop();
        log.log(Level.INFO, "\uD83D\uDED1Stopped light client!");
    }
}
