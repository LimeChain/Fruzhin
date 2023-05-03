package com.limechain.network.protocol.blockannounce;

import com.limechain.network.kad.KademliaService;
import com.limechain.network.protocol.blockannounce.scale.BlockAnnounceHandshake;
import io.emeraldpay.polkaj.types.Hash256;
import io.ipfs.multihash.Multihash;
import io.libp2p.core.Host;
import io.libp2p.core.PeerId;
import io.libp2p.core.multiformats.Multiaddr;
import io.libp2p.protocol.Ping;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.peergos.HostBuilder;

import java.util.List;
import java.util.Random;

public class BlockAnnounceTest {
    //@Disabled("This is an integration test!")
    @Test
    public void receivesNotifications() {
        Host senderNode = null;
        try {
            HostBuilder hostBuilder1 =
                    (new HostBuilder()).generateIdentity().listenLocalhost(
                            10000 + new Random().nextInt(50000));

            var blockAnnounce = new BlockAnnounce("/dot/block-announces/1", new BlockAnnounceProtocol());
            var kademliaService = new KademliaService("/dot/kad",
                    Multihash.deserialize(hostBuilder1.getPeerId().getBytes()), true, false);

            hostBuilder1.addProtocols(List.of(new Ping(), blockAnnounce, kademliaService.getProtocol()));
            senderNode = hostBuilder1.build();

            senderNode.start().join();

            kademliaService.host = senderNode;

            //Polkadot
            var peerId = PeerId.fromBase58("12D3KooWMSYMPsrD81abMMx8YsZp41Mo1L9UXQRAimitPTP2VsK7");

            var receivers = new String[]{
                    "/ip4/127.0.0.1/tcp/30333/p2p/" + peerId.toBase58()
            };

            kademliaService.connectBootNodes(receivers);

            var handshake = new BlockAnnounceHandshake() {{
                nodeRole = 4;
                bestBlockHash = Hash256.from("0x7b22fc4469863c9671686c189a3238708033d364a77ba8d83e78777e7563f346");
                bestBlock = "0";
                genesisBlockHash = Hash256.from(
                        "0x7b22fc4469863c9671686c189a3238708033d364a77ba8d83e78777e7563f346");
            }};

            System.out.println("PeerID: " + senderNode.getPeerId());
            Multiaddr[] addr = senderNode.getAddressBook().get(peerId)
                    .join().stream()
                    .filter(address -> !address.toString().contains("/ws") && !address.toString().contains("/wss"))
                    .toList()
                    .toArray(new Multiaddr[0]);

            if (addr.length == 0)
                throw new IllegalStateException("No addresses known for peer " + peerId);

            //blockAnnounce.sendHandshake(senderNode, senderNode.getAddressBook(), peerId, handshake);

            Thread.sleep(60000);
        } catch (
                InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            if (senderNode != null) {
                senderNode.stop();
            }
        }
    }

}
