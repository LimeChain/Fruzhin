package com.limechain.network.protocol.warp.dto;

import com.limechain.chain.lightsyncstate.Authority;
import com.limechain.network.kad.KademliaService;
import com.limechain.network.protocol.warp.WarpSync;
import com.limechain.network.protocol.warp.WarpSyncProtocol;
import io.emeraldpay.polkaj.types.Hash256;
import io.ipfs.multibase.Base58;
import io.ipfs.multihash.Multihash;
import io.libp2p.core.Host;
import io.libp2p.core.PeerId;
import io.libp2p.protocol.Ping;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.peergos.HostBuilder;

import java.math.BigInteger;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class WarpSyncJustificationTest {

    @Test
    public void verifyAgainstLocalNode() {
        Authority authority = new Authority(Hash256.from("0x88dc3417d5058ec4b4503e0c12ea1a0a89be200fe98922423d4334014fa6b0ee"), 10);
        Authority[] authorities = new Authority[]{authority};

        boolean isVerified = verifyWarpSyncJustification("12D3KooWGJZ5TfuHp7U2Dw12JdhjDvakMJCbHeoFoZGaYxEute4W",
                "/ip4/127.0.0.1/tcp/30333/p2p/12D3KooWGJZ5TfuHp7U2Dw12JdhjDvakMJCbHeoFoZGaYxEute4W",
                "0x7b22fc4469863c9671686c189a3238708033d364a77ba8d83e78777e7563f346",
                authorities);
        assertTrue(isVerified);
    }

    @Disabled("Cannot get authority set of the online node for now.")
    @Test
    public void verifyAgainstOnlineNode() {
        boolean isVerified = verifyWarpSyncJustification("12D3KooWQz2q2UWVCiy9cFX1hHYEmhSKQB2hjEZCccScHLGUPjcc",
                "/dns/p2p.1.polkadot.network/tcp/30333/p2p/12D3KooWQz2q2UWVCiy9cFX1hHYEmhSKQB2hjEZCccScHLGUPjcc",
                "0xfcb0a6594924887d7c49083df1719c9b23918610804eb9edc201d9c6d6493f3a",
                new Authority[10]);
        assertTrue(isVerified);
    }

    public boolean verifyWarpSyncJustification(String peerId, String peerAddress, String blockHash, Authority[] authorities) {
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
            var receivers = new String[]{
                    peerAddress
            };

            kademliaService.connectBootNodes(receivers);

            var response = warpSync.warpSyncRequest(senderNode, senderNode.getAddressBook(), new PeerId(Base58.decode(peerId)),
                    blockHash
            );

            WarpSyncFragment[] fragments = response.getFragments();
            for (WarpSyncFragment fragment : fragments) {
                boolean verified = fragment.getJustification().verify(authorities, BigInteger.valueOf(10));
                if (!verified) return false;
            }
            return true;
        } finally {

            if (senderNode != null) {
                senderNode.stop();
            }
        }
    }
}
