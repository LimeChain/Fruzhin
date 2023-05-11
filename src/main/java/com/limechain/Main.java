package com.limechain;

import com.limechain.lightclient.LightClient;
import com.limechain.rpc.http.server.HttpRpc;
import com.limechain.rpc.ws.server.WebSocketRPC;
import sun.misc.Signal;

public class Main {
    public static void main(String[] args) {
        HttpRpc httpRpc = new HttpRpc();
        WebSocketRPC wsRpc = new WebSocketRPC();
        LightClient client = new LightClient(args, httpRpc, wsRpc);

        client.start();
        Signal.handle(new Signal("INT"), signal -> client.stop());
    }
}