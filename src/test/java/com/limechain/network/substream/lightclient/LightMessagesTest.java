package com.limechain.network.substream.lightclient;

import io.libp2p.core.Host;
import io.libp2p.core.multiformats.Multiaddr;
import io.libp2p.protocol.Ping;
import org.junit.jupiter.api.Test;
import org.peergos.HostBuilder;

import java.util.List;
import java.util.Objects;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class LightMessagesTest {

    @Test
    public void remoteFunctions_return_correctData() {
        Host senderNode = null;
        Host receiverNode = null;

        try {
            HostBuilder hostBuilder1 =
                    (new HostBuilder()).generateIdentity().listenLocalhost(10000 + new Random().nextInt(50000));

            var lightMessages1 = new LightMessages(new LightMessagesProtocol(new LightMessagesEngine()));
            hostBuilder1.addProtocols(List.of(new Ping(), lightMessages1));
            senderNode = hostBuilder1.build();

            HostBuilder hostBuilder2 =
                    (new HostBuilder()).generateIdentity().listenLocalhost(10000 + new Random().nextInt(50000));
            var lightMessages2 = new LightMessages(new LightMessagesProtocol(new LightMessagesEngine()));
            hostBuilder2.addProtocols(List.of(new Ping(), lightMessages2));
            receiverNode = hostBuilder2.build();

            senderNode.start().join();
            receiverNode.start().join();

            Multiaddr address2 = receiverNode.listenAddresses().get(0);
            senderNode.getAddressBook().addAddrs(Objects.requireNonNull(address2.getPeerId()), 0, address2).join();
            
            var remoteCallResponse =
                    lightMessages1.remoteCallRequest(senderNode, senderNode.getAddressBook(), receiverNode.getPeerId(),
                            "0x123",
                            "testMethod", "0x");
            assertTrue(remoteCallResponse.hasRemoteCallResponse());

            var remoteReadResponse =
                    lightMessages1.remoteReadRequest(senderNode, senderNode.getAddressBook(), receiverNode.getPeerId(),
                            "0x123",
                            new String[]{"0x321"});
            assertTrue(remoteReadResponse.hasRemoteReadResponse());

            var remoteReadChildResponse =
                    lightMessages1.remoteReadChildRequest(senderNode, senderNode.getAddressBook(),
                            receiverNode.getPeerId(),
                            "0x123",
                            "0xchild", new String[]{"0x321"});
            assertTrue(remoteReadChildResponse.hasRemoteReadResponse());
        } finally {

            if (senderNode != null) {
                senderNode.stop();
            }

            if (receiverNode != null) {
                receiverNode.stop();
            }
        }
    }
}
