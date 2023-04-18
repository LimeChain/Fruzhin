package com.limechain.network.protocol.blockannounce;

import com.limechain.network.kad.KademliaService;
import com.limechain.network.protocol.blockannounce.scale.BlockAnnounceHandShake;
import io.emeraldpay.polkaj.types.Hash256;
import io.ipfs.multihash.Multihash;
import io.libp2p.core.Host;
import io.libp2p.core.PeerId;
import io.libp2p.protocol.Ping;
import org.junit.jupiter.api.Test;
import org.peergos.HostBuilder;

import java.util.List;
import java.util.Random;

public class BlockAnnounceTest {
    @Test
    public void receivesNotifications() {
        Host senderNode = null;
        try {
            HostBuilder hostBuilder1 =
                    (new HostBuilder()).generateIdentity().listenLocalhost(10000 + new Random().nextInt(50000));

            var blockAnnounce = new BlockAnnounce("/dot/block-announces/1", new BlockAnnounceProtocol());
            var kademliaService = new KademliaService("/dot/kad",
                    Multihash.deserialize(hostBuilder1.getPeerId().getBytes()), false, true);

            hostBuilder1.addProtocols(List.of(new Ping(), blockAnnounce, kademliaService.getProtocol()));
            senderNode = hostBuilder1.build();

            senderNode.start().join();

            kademliaService.host = senderNode;
            var peerId = PeerId.fromBase58("12D3KooWLiGEgzQy8XRp825ZGDRxhcWdvPzC5QydUaNkzkp9ffGN");
            var receivers = new String[]{
                    "/ip4/127.0.0.1/tcp/30333/p2p/12D3KooWLiGEgzQy8XRp825ZGDRxhcWdvPzC5QydUaNkzkp9ffGN"
//                    "/ip4/127.0.0.1/tcp/7001/p2p/12D3KooWRWPEuqV2ECJfRvqG7Dj1Qk8NC8jpsQZuWAoySXJLqkHA"
//                    "/dns/p2p.0.polkadot.network/tcp/30333/p2p/12D3KooWHsvEicXjWWraktbZ4MQBizuyADQtuEGr3NbDvtm5rFA5",
            };

            kademliaService.connectBootNodes(receivers);
            kademliaService.findNewPeers();
            blockAnnounce.sendHandshake(senderNode, senderNode.getAddressBook(), peerId, new BlockAnnounceHandShake() {{
                nodeRole = 1;
                bestBlockHash = Hash256.from("0xbbce82af7e14f84ed09c051f25384ba80adf6b5a5fbc0086c0eea6986ad9d82a");
                bestBlock = "15072200";
                genesisBlockHash = Hash256.from("0x91b171bb158e2d3848fa23a9f1c25182fb8e20313b2c1eb49219da7a70ce90c3");
            }});

            Thread.sleep(25000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            if (senderNode != null) {
                senderNode.stop();
            }
        }
    }

}
