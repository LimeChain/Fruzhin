package com.limechain.network.protocol.grandpa;

import com.limechain.network.ConnectionManager;
import com.limechain.network.protocol.NetworkService;
import io.libp2p.core.Host;
import io.libp2p.core.PeerId;
import io.libp2p.core.Stream;

public class GrandpaService extends NetworkService<Grandpa> {
    ConnectionManager connectionManager = ConnectionManager.getInstance();
    public GrandpaService(String protocolId) {
        this.protocol = new Grandpa(protocolId, new GrandpaProtocol());
    }

    public void sendNeighbourMessage(Host us, PeerId peerId) {
        Stream grandpaStream = connectionManager.getPeerInfo(peerId).getGrandpaStreams().getInitiator();
        if (grandpaStream != null) {
            GrandpaController controller = new GrandpaController(grandpaStream);
            controller.sendNeighbourMessage();
        } else {
            GrandpaController controller = this.protocol.dialPeer(us, peerId, us.getAddressBook());
            controller.sendHandshake();
        }
    }
}
