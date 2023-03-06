package com.limechain;

import com.limechain.lightClient.LightClient;
import com.limechain.rpc.server.RPC;
import com.limechain.ws.server.WebSocketRPC;

public class Main {
    public static void main (String[] args) {
        RPC rpc = new RPC();
        WebSocketRPC wsrpc = new WebSocketRPC();
        LightClient client = new LightClient(args, rpc, wsrpc);
        client.start();
    }
}