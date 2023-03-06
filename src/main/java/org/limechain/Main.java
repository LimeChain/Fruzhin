package org.limechain;

import org.limechain.lightClient.LightClient;
import org.limechain.rpc.RPC;

public class Main {
    public static void main (String[] args) {
        RPC rpc = new RPC();
        LightClient client = new LightClient(rpc);
        client.start();
    }
}