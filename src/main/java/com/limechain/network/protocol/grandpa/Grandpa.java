package com.limechain.network.protocol.grandpa;

import com.limechain.network.StrictProtocolBinding;
import io.libp2p.core.AddressBook;
import io.libp2p.core.Host;
import io.libp2p.core.PeerId;

public class Grandpa extends StrictProtocolBinding<GrandpaController> {
    public Grandpa(String protocolId, GrandpaProtocol protocol) {
        super(protocolId, protocol);
    }

    public void sendHandshake(Host us, AddressBook addrs, PeerId peer) {
        GrandpaController controller = dialPeer(us, peer, addrs);
        controller.sendHandshake();
    }
}
