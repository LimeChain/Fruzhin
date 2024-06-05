package com.limechain.network.protocol.state;

import com.limechain.network.kad.KademliaService;
import com.limechain.network.protocol.sync.pb.SyncMessage;
import com.limechain.utils.RandomGenerationUtils;
import io.ipfs.multiaddr.MultiAddress;
import io.ipfs.multihash.Multihash;
import io.libp2p.core.Host;
import io.libp2p.core.PeerId;
import io.libp2p.protocol.Ping;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.peergos.HostBuilder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class StateTest {
    public static final String PEER_ID = "12D3KooWSz8r2WyCdsfWHgPyvD8GKQdJ1UAiRmrcrs8sQB3fe2KU";
    public static final String PEER_URL = "/dns/polkadot-bootnode-0.polkadot.io/tcp/30333/p2p/";
    private Host senderNode = null;
    private KademliaService kademliaService = null;
    private StateMessages stateService = null;

    @BeforeAll
    public void init() {
        MultiAddress address = RandomGenerationUtils.generateRandomAddress();
        HostBuilder hostBuilder = new HostBuilder()
                .generateIdentity()
                .listen(List.of(address));

        stateService = new StateMessages("/dot/state/2", new StateProtocol());
        kademliaService = new KademliaService("/dot/kad",
                Multihash.deserialize(hostBuilder.getPeerId().getBytes()), false, true);

        hostBuilder.addProtocols(List.of(new Ping(), kademliaService.getProtocol(), stateService));
        senderNode = hostBuilder.build();

        senderNode.start().join();

        kademliaService.setHost(senderNode);
    }

    @AfterAll
    public void stopNode() {
        if (senderNode != null) {
            senderNode.stop();
        }
    }

    @Test
    @Disabled("Test works, but requires block within the last 256 finalized ones or in other words kind if a integration test")
    void remoteFunctions_return_correctData() {
        var peerId = PeerId.fromBase58(PEER_ID);
        var receivers = new String[]{PEER_URL + PEER_ID};

        int connectedNodes = kademliaService.connectBootNodes(receivers);
        int expectedConnectedNodes = 1;
        assertEquals(expectedConnectedNodes, connectedNodes);

        SyncMessage.StateResponse response =
                stateService.remoteStateRequest(senderNode, senderNode.getAddressBook(), peerId,
                        "0xc430332fe17cedb79ae58387ad883f369fb1028683680db0b12ba60248769f6c"
                );

        assertNotNull(response);
    }
}
