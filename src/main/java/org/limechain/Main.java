package org.limechain;

import org.limechain.lightClient.LightClient;
import org.limechain.rpc.server.RPC;
import org.limechain.ws.server.WebSocketRPC;

public class Main {
    public static void main (String[] args) {
        RPC rpc = new RPC();
        WebSocketRPC wsrpc = new WebSocketRPC();
        LightClient client = new LightClient(args, rpc, wsrpc);
        client.start();
    }
}