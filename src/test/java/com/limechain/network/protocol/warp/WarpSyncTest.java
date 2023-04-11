package com.limechain.network.protocol.warp;

import com.limechain.network.kad.KademliaService;
import io.ipfs.multihash.Multihash;
import io.libp2p.core.Host;
import io.libp2p.core.PeerId;
import io.libp2p.protocol.Ping;
import org.junit.jupiter.api.Test;
import org.peergos.HostBuilder;

import java.util.List;
import java.util.Random;

public class WarpSyncTest {

    @Test
    public void remoteFunctions_return_correctData() {
        Host senderNode = null;

        try {
            HostBuilder hostBuilder1 =
                    (new HostBuilder()).generateIdentity().listenLocalhost(10000 + new Random().nextInt(50000));

            var warpSync1 = new WarpSync(new WarpSyncProtocol());
            var kademliaService = new KademliaService("/dot/kad",
                    Multihash.deserialize(hostBuilder1.getPeerId().getBytes()), false);

            hostBuilder1.addProtocols(List.of(new Ping(), warpSync1, kademliaService.getDht()));
            senderNode = hostBuilder1.build();

            senderNode.start().join();

            kademliaService.setHost(senderNode);
            var peerId = PeerId.fromBase58("12D3KooWSRQFbXzBaP3Ec4Ayb1PKcdc7DDFqdjBZhPH2qeQZUCyp");
            var receivers = new String[]{
                    "/ip4/127.0.0.1/tcp/30333/p2p/12D3KooWSRQFbXzBaP3Ec4Ayb1PKcdc7DDFqdjBZhPH2qeQZUCyp"
//                    "/dns/p2p.0.polkadot.network/tcp/30333/p2p/12D3KooWHsvEicXjWWraktbZ4MQBizuyADQtuEGr3NbDvtm5rFA5",
//                    "/dns/p2p.1.polkadot.network/tcp/30333/p2p/12D3KooWQz2q2UWVCiy9cFX1hHYEmhSKQB2hjEZCccScHLGUPjcc",
//                    "/dns/p2p.2.polkadot.network/tcp/30333/p2p/12D3KooWNHxjYbDLLbDNZ2tq1kXgif5MSiLTUWJKcDdedKu4KaG8",
//                    "/dns/p2p.3.polkadot.network/tcp/30333/p2p/12D3KooWGJQysxrQcSvUWWNw88RkqYvJhH3ZcDpWJ8zrXKhLP5Vr",
//                    "/dns/p2p.4.polkadot.network/tcp/30333/p2p/12D3KooWKer8bYqpYjwurVABu13mkELpX2X7mSpEicpjShLeg7D6",
//                    "/dns/p2p.5.polkadot.network/tcp/30333/p2p/12D3KooWSRjL9LcEQd5u2fQTbyLxTEHq1tUFgQ6amXSp8Eu7TfKP",
//                    "/dns/cc1-0.parity.tech/tcp/30333/p2p/12D3KooWSz8r2WyCdsfWHgPyvD8GKQdJ1UAiRmrcrs8sQB3fe2KU",
//                    "/dns/cc1-1.parity.tech/tcp/30333/p2p/12D3KooWFN2mhgpkJsDBuNuE5427AcDrsib8EoqGMZmkxWwx3Md4"
            };

            // TODO: connectBootNodes to return number of successful connection in order to validate if > 0
            kademliaService.connectBootNodes(receivers);

            var response = warpSync1.warpSyncRequest(senderNode, senderNode.getAddressBook(), peerId,
                    //TODO: This should come from the chain spec light client state
//                    "0x91b171bb158e2d3848fa23a9f1c25182fb8e20313b2c1eb49219da7a70ce90c3"
                    "b71e3ddbfe2b3d1cb534563493b779acbb08ca28019f75cc03c8eeaf55751042"
            );
            System.out.println("Response: " + response);
            System.out.println("Done");
        } finally {

            if (senderNode != null) {
                senderNode.stop();
            }
        }
    }

}
