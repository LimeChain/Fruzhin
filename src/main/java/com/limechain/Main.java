package com.limechain;

import com.limechain.network.kad.KademliaService;

import java.util.List;

public class Main {
    public static void main(String[] args) {
//        HttpRpc httpRpc = new HttpRpc();
//        WebSocketRPC wsRpc = new WebSocketRPC();
//        LightClient client = new LightClient(args, httpRpc, wsRpc);
//        client.start();
        List<String> boostrapNodes = List.of(
                "/ip4/127.0.0.1/tcp/7001/p2p/12D3KooWJjYufseGQH4hLT97hPBfCv6qt5rcZLLqvEyWyDdV1Nxr",
                "/ip4/127.0.0.1/tcp/7002/p2p/12D3KooWPtm13PEe1MTd36G9dMz9WfQHvD94HDR7XLdivHxiwZxA"
//                "/ip4/127.0.0.1/tcp/7003/p2p/12D3KooWBaMbhR9643K6aUDfCCuPqogr283CHhQhjbxhb5EFQRTs"
        );

        var kadSrvc = new KademliaService("/dot/kad", boostrapNodes);

    }
}