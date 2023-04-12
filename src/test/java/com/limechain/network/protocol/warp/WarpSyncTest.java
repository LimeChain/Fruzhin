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

    @Disabled("This is an integration test")
    @Test
    public void remoteFunctions_return_correctData() {
        Host senderNode = null;

        try {
            HostBuilder hostBuilder1 =
                    (new HostBuilder()).generateIdentity().listenLocalhost(10000 + new Random().nextInt(50000));

            var warpSync = new WarpSync("/dot/sync/warp", new WarpSyncProtocol());
            var kademliaService = new KademliaService("/dot/kad",
                    Multihash.deserialize(hostBuilder1.getPeerId().getBytes()), false);

            hostBuilder1.addProtocols(List.of(new Ping(), warpSync, kademliaService.getProtocol()));
            senderNode = hostBuilder1.build();

            senderNode.start().join();

            kademliaService.host = senderNode;
            var peerId = PeerId.fromBase58("12D3KooWHsvEicXjWWraktbZ4MQBizuyADQtuEGr3NbDvtm5rFA5");
            var receivers = new String[]{
                    "/dns/p2p.0.polkadot.network/tcp/30333/p2p/12D3KooWHsvEicXjWWraktbZ4MQBizuyADQtuEGr3NbDvtm5rFA5",
            };

            kademliaService.connectBootNodes(receivers);

            var response = warpSync.warpSyncRequest(senderNode, senderNode.getAddressBook(), peerId,
                    "b71e3ddbfe2b3d1cb534563493b779acbb08ca28019f75cc03c8eeaf55751042"
            );
            assertNotNull(response);
        } finally {

            if (senderNode != null) {
                senderNode.stop();
            }
        }
    }

}
