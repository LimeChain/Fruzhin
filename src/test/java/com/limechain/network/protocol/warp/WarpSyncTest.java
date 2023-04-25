package com.limechain.network.protocol.warp;

import com.limechain.network.kad.KademliaService;
import io.ipfs.multihash.Multihash;
import io.libp2p.core.Host;
import io.libp2p.core.PeerId;
import io.libp2p.protocol.Ping;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.peergos.HostBuilder;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class WarpSyncTest {
    @Disabled("This is an integration test. It should be run manually.")
    @Test
    public void remoteFunctions_return_correctData() {
        Host senderNode = null;

        try {
            HostBuilder hostBuilder1 =
                    (new HostBuilder()).generateIdentity().listenLocalhost(10000 + new Random().nextInt(50000));

            var warpSync = new WarpSync("/dot/sync/warp", new WarpSyncProtocol());
            var kademliaService = new KademliaService("/dot/kad",
                    Multihash.deserialize(hostBuilder1.getPeerId().getBytes()), false, false);

            hostBuilder1.addProtocols(List.of(new Ping(), warpSync, kademliaService.getProtocol()));
            senderNode = hostBuilder1.build();

            senderNode.start().join();

            kademliaService.host = senderNode;
            var peerId = PeerId.fromBase58("12D3KooWMWgGa4jQKyyC2zhZo2DrjP9yZmC4b75Uoa9ZzjJxTMSE");
            var receivers = new String[]{
                    "/ip4/127.0.0.1/tcp/30333/p2p/12D3KooWMWgGa4jQKyyC2zhZo2DrjP9yZmC4b75Uoa9ZzjJxTMSE",
            };

            kademliaService.connectBootNodes(receivers);

            var response = warpSync.warpSyncRequest(senderNode, senderNode.getAddressBook(), peerId,
                    "0xb3a0f1e3c06de74c9aedb3e4b129b570f778dd82005b2a84177dfb8a1b3751d0"
            );
            assertNotNull(response);
        } finally {

            if (senderNode != null) {
                senderNode.stop();
            }
        }
    }

}
