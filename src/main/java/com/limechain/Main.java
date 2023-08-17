package com.limechain;

import com.limechain.lightclient.LightClient;
import com.limechain.rpc.ws.server.WebSocketRPC;
import sun.misc.Signal;

public class Main {
    public static void main(String[] args) {
        WebSocketRPC wsRpc = new WebSocketRPC();
        LightClient client = new LightClient(args, wsRpc);

        client.start();
        Signal.handle(new Signal("INT"), signal -> client.stop());
    }
}