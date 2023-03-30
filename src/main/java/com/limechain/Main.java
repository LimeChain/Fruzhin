package com.limechain;

import com.limechain.lightclient.LightClient;
import com.limechain.rpc.http.server.HttpRpc;
import com.limechain.rpc.ws.server.WebSocketRPC;
import org.peergos.protocol.dnsaddr.DnsAddr;

public class Main {
    public static void main(String[] args) {
//        HttpRpc httpRpc = new HttpRpc();
//        WebSocketRPC wsRpc = new WebSocketRPC();
//        LightClient client = new LightClient(args, httpRpc, wsRpc);
//        client.start();
        var dadr = DnsAddr.resolve("/dnsaddr/0.westend.paritytech.net/tcp/30333/p2p/12D3KooWKer94o1REDPtAhjtYR4SdLehnSrN8PEhBnZm5NBoCrMC");
        System.out.println(dadr);
    }
}