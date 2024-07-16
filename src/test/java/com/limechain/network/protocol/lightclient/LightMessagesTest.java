package com.limechain.network.protocol.lightclient;

import com.limechain.network.kad.KademliaService;
import com.limechain.network.protocol.lightclient.pb.LightClientMessage;
import com.limechain.utils.RandomGenerationUtils;
import io.ipfs.multiaddr.MultiAddress;
import io.ipfs.multihash.Multihash;
import io.libp2p.core.Host;
import io.libp2p.core.PeerId;
import io.libp2p.protocol.Ping;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.peergos.HostBuilder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class LightMessagesTest {
    @Disabled("This is an integration test")
    @Test
    void remoteReadRequest_return_response() {
        Host senderNode = null;
        try {
            MultiAddress address = RandomGenerationUtils.generateRandomAddress();
            HostBuilder hostBuilder1 = new HostBuilder()
                    .generateIdentity()
                    .listen(List.of(address));

            var lightMessages = new LightMessages("/dot/light/2", new LightMessagesProtocol());
            var kademliaService = new KademliaService("/dot/kad",
                    Multihash.deserialize(hostBuilder1.getPeerId().getBytes()), false, true);

            hostBuilder1.addProtocols(List.of(new Ping(), lightMessages, kademliaService.getProtocol()));
            senderNode = hostBuilder1.build();

            senderNode.start().join();

            kademliaService.setHost(senderNode);
            var peerId = PeerId.fromBase58("12D3KooWPGSssFbR4XvuSfvu7Rdq4MUv82HdsygXZ4nRhEw3vJpC");
            var receivers = new String[]{
                    "/ip4/127.0.0.1/tcp/30333/p2p/12D3KooWPGSssFbR4XvuSfvu7Rdq4MUv82HdsygXZ4nRhEw3vJpC"
//                    "/dns/p2p.0.polkadot.network/tcp/30333/p2p/12D3KooWHsvEicXjWWraktbZ4MQBizuyADQtuEGr3NbDvtm5rFA5",
            };

            // TODO: connectBootNodes to return number of successful connection in order to validate if > 0
            kademliaService.connectBootNodes(receivers);

            LightClientMessage.Response response = lightMessages.remoteReadRequest(
                    senderNode,
                    peerId,
                    "202d85e7911b81e7e704be791b6a2147dc37b571bd311abe5dbf6ab3860dc4b8",
                    new String[]{"9c5d795d0297be56027a4b2464e333979c5d795d0297be56027a4b2464e333977a2dce72ec5f24ed58baf131ea24762f3947ac46"}
            );

            assertNotNull(response);
        } finally {
            if (senderNode != null) {
                senderNode.stop();
            }
        }
    }
}
