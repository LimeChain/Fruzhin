package org.limechain;

import org.limechain.chain.ChainService;
import org.limechain.config.AppConfig;
import org.limechain.lightClient.LightClient;
import org.limechain.rpc.RPC;

public class Main {
    public static void main (String[] args) {
        AppConfig appConfig = new AppConfig(args);
        ChainService chainService = new ChainService(appConfig);
        RPC rpc = new RPC();
        LightClient client = new LightClient(chainService, rpc);
        client.start();
//        client.stop();
    }
}