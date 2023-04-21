package com.limechain.network.protocol.lightclient;

import com.limechain.network.kad.KademliaService;
import com.limechain.network.protocol.lightclient.pb.LightClientMessage;
import io.ipfs.multihash.Multihash;
import io.libp2p.core.Host;
import io.libp2p.core.PeerId;
import io.libp2p.protocol.Ping;
import org.junit.jupiter.api.Test;
import org.peergos.HostBuilder;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertNotNull;

//CHECKSTYLE.OFF
public class LightMessagesTest {

    @Test
    public void remoteReadRequest_return_response() {
        Host senderNode = null;
        try {
            HostBuilder hostBuilder1 =
                    (new HostBuilder()).generateIdentity().listenLocalhost(10000 + new Random().nextInt(50000));

            var lightMessages = new LightMessages("/dot/light/2", new LightMessagesProtocol());
            var kademliaService = new KademliaService("/dot/kad",
                    Multihash.deserialize(hostBuilder1.getPeerId().getBytes()), false);

            hostBuilder1.addProtocols(List.of(new Ping(), lightMessages, kademliaService.getProtocol()));
            senderNode = hostBuilder1.build();

            senderNode.start().join();

            kademliaService.host = senderNode;
            var peerId = PeerId.fromBase58("12D3KooWMfNeF5kTufr24Mgp76D8QgE6R5vGmbatVjP8Ls7HfQAb");
            var receivers = new String[]{
                    "/ip4/127.0.0.1/tcp/30333/p2p/12D3KooWMfNeF5kTufr24Mgp76D8QgE6R5vGmbatVjP8Ls7HfQAb",
            };

            // TODO: connectBootNodes to return number of successful connection in order to validate if > 0
            kademliaService.connectBootNodes(receivers);

            LightClientMessage.Response response = lightMessages.remoteReadRequest(
                    senderNode,
                    kademliaService.host.getAddressBook(),
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
//CHECKSTYLE.ON
