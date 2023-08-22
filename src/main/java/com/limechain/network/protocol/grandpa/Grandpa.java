package com.limechain.network.protocol.grandpa;

import com.limechain.network.ConnectionManager;
import com.limechain.network.StrictProtocolBinding;
import io.libp2p.core.AddressBook;
import io.libp2p.core.Host;
import io.libp2p.core.PeerId;
import io.libp2p.core.Stream;

public class Grandpa extends StrictProtocolBinding<GrandpaController> {
    ConnectionManager connectionManager = ConnectionManager.getInstance();

    public Grandpa(String protocolId, GrandpaProtocol protocol) {
        super(protocolId, protocol);
    }

    public void sendHandshake(Host us, AddressBook addrs, PeerId peer) {
        GrandpaController controller = dialPeer(us, peer, addrs);
        controller.sendHandshake();
    }

    public void sendNeighbourMessage(Host us, AddressBook addrs, PeerId peer) {
        Stream grandpaStream = connectionManager.getPeerInfo(peer).getGrandpaStreams().getInitiator();
        if (grandpaStream != null) {
            ((GrandpaProtocol) this.getProtocol()).sendNeighbourMessage(grandpaStream);
        } else {
            sendHandshake(us, addrs, peer);
        }
    }
}
