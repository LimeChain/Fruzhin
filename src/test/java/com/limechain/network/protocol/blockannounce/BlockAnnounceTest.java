package com.limechain.network.protocol.blockannounce;

import com.limechain.network.kad.KademliaService;
import com.limechain.network.protocol.blockannounce.scale.BlockAnnounceHandShake;
import io.emeraldpay.polkaj.types.Hash256;
import io.ipfs.multihash.Multihash;
import io.libp2p.core.Host;
import io.libp2p.core.PeerId;
import io.libp2p.core.StreamPromise;
import io.libp2p.core.multiformats.Multiaddr;
import io.libp2p.protocol.Ping;
import org.junit.jupiter.api.Test;
import org.peergos.HostBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class BlockAnnounceTest {
    @Test
    public void receivesNotifications() {
        Host senderNode = null;
        try {
            HostBuilder hostBuilder1 =
                    (new HostBuilder()).generateIdentity().listenLocalhost(10000 + new Random().nextInt(50000));

            var blockAnnounce = new BlockAnnounce("/dot/block-announces/1", new BlockAnnounceProtocol());
            var kademliaService = new KademliaService("/dot/kad",
                    Multihash.deserialize(hostBuilder1.getPeerId().getBytes()), true, false);

            hostBuilder1.addProtocols(List.of(new Ping(), blockAnnounce, kademliaService.getProtocol()));
            senderNode = hostBuilder1.build();

            senderNode.start().join();

            kademliaService.host = senderNode;
            var peerId = PeerId.fromBase58("12D3KooWC1gBkDciExkQUNissYmEHdGLkxYAZMhq5FsgxAS6HB9v");
            var receivers = new String[]{
                    "/ip4/127.0.0.1/tcp/30333/p2p/12D3KooWC1gBkDciExkQUNissYmEHdGLkxYAZMhq5FsgxAS6HB9v",
            };
            kademliaService.connectBootNodes(receivers);

            var handShake = new BlockAnnounceHandShake() {{
                nodeRole = 4;
                bestBlockHash = Hash256.from("0x1f7a1b28529651bb50b3ef4304f82fbc72bc791b9b838920df2fa96eabe011aa");
                bestBlock = "3";
                genesisBlockHash = Hash256.from("0xb6d36a6766363567d2a385c8b5f9bd93b223b8f42e54aa830270edcf375f4d63");
            }};

            System.out.println("PeerID: " + senderNode.getPeerId());
            Multiaddr[] addr = senderNode.getAddressBook().get(peerId)
                    .join().stream()
                    .filter(address -> !address.toString().contains("/ws") && !address.toString().contains("/wss"))
                    .toList()
                    .toArray(new Multiaddr[0]);

            if (addr.length == 0)
                throw new IllegalStateException("No addresses known for peer " + peerId);

            StreamPromise<BlockAnnounceController> senderController = senderNode.newStream(new ArrayList<>() {{
                add("/dot/block-announces/1");
            }}, peerId, addr);

            senderController.getController().join().sendHandshake(handShake);

            System.out.println(senderNode.getStreams().stream().map(s -> s.getProtocol().join()).collect(Collectors.joining(", ")));

//            while (true) {
//                Thread.sleep(2000);
//                System.out.println(senderNode.getStreams().stream().map(s -> s.getProtocol().join()).collect(Collectors.joining(", ")));
//
//                var blockAnnStreams = senderNode.getStreams().stream().filter(s -> s.getProtocol().join().equals("/dot/block-announces/1")).toList();
//
//                if (blockAnnStreams.size() == 0) {
//                    continue;
//                }
//
//                Host finalSenderNode = senderNode;
//
//                blockAnnStreams.forEach(s -> {
//                    if (s.isInitiator()) {
//                        return;
//                    }
//                    Multiaddr[] addr2 = finalSenderNode.getAddressBook().get(peerId)
//                            .join().stream()
//                            .filter(address -> !address.toString().contains("/ws") && !address.toString().contains("/wss"))
//                            .toList()
//                            .toArray(new Multiaddr[0]);
//
//                    if (addr2.length == 0)
//                        throw new IllegalStateException("No addresses known for peer " + peerId);
//
//                    StreamPromise<BlockAnnounceController> senderController2 = finalSenderNode.newStream(new ArrayList<>() {{
//                        add("/dot/block-announces/1");
//                    }}, peerId, addr2);
//
//                    senderController2.getController().join().sendHandshake(handShake);
//                });
//                break;
//            }

            System.out.println(senderNode.getStreams().stream().map(s -> s.getProtocol().join()).collect(Collectors.joining(", ")));
            Thread.sleep(25000);
            System.out.println(senderNode.getStreams().stream().map(s -> s.getProtocol().join()).collect(Collectors.joining(", ")));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            if (senderNode != null) {
                senderNode.stop();
            }
        }
    }

}
