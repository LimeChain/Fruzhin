package com.limechain;

import com.limechain.lightclient.LightClient;
import com.limechain.rpc.http.server.HttpRpc;
import com.limechain.rpc.ws.server.WebSocketRPC;

public class Main {
    public static void main (String[] args) {
        HttpRpc httpRpc = new HttpRpc();
        WebSocketRPC wsRpc = new WebSocketRPC();
        LightClient client = new LightClient(args, httpRpc, wsRpc);
        client.start();
    }
}