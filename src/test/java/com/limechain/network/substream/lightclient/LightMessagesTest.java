package com.limechain.network.substream.lightclient;

import com.limechain.network.kad.KademliaService;
import io.ipfs.multihash.Multihash;
import io.libp2p.core.Host;
import io.libp2p.core.PeerId;
import io.libp2p.protocol.Ping;
import org.junit.jupiter.api.Test;
import org.peergos.HostBuilder;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class LightMessagesTest {

    @Test
    public void remoteReadRequest_return_response() {
        Host senderNode = null;
        try {
            HostBuilder hostBuilder1 =
                    (new HostBuilder()).generateIdentity().listenLocalhost(10000 + new Random().nextInt(50000));

            var lightMessages = new LightMessages(new LightMessagesProtocol(new LightMessagesEngine()));
            var kademliaService = new KademliaService("/dot/kad",
                    Multihash.deserialize(hostBuilder1.getPeerId().getBytes()), false);

            hostBuilder1.addProtocols(List.of(new Ping(), lightMessages, kademliaService.getDht()));
            senderNode = hostBuilder1.build();

            senderNode.start().join();

            kademliaService.setHost(senderNode);
            var peerId = PeerId.fromBase58("12D3KooWHsvEicXjWWraktbZ4MQBizuyADQtuEGr3NbDvtm5rFA5");
            var receivers = new String[]{
                    "/dns/p2p.0.polkadot.network/tcp/30333/p2p/12D3KooWHsvEicXjWWraktbZ4MQBizuyADQtuEGr3NbDvtm5rFA5",
            };

            // TODO: connectBootNodes to return number of successful connection in order to validate if > 0
            kademliaService.connectBootNodes(receivers);

            var response = lightMessages.remoteReadRequest(senderNode, kademliaService.getHost().getAddressBook(), peerId,
                    "91b171bb158e2d3848fa23a9f1c25182fb8e20313b2c1eb49219da7a70ce90c3",
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
