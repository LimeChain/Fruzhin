package com.limechain;

import com.limechain.lightclient.LightClient;
import com.limechain.rpc.server.RpcApp;
import sun.misc.Signal;

public class Main {
    public static void main(String[] args) {
        RpcApp wsRpc = new RpcApp();
        LightClient client = new LightClient(args, wsRpc);

        client.start();
        Signal.handle(new Signal("INT"), signal -> client.stop());
    }
}