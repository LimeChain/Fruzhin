package com.limechain.network.protocol.grandpa;

import com.limechain.network.ConnectionManager;
import com.limechain.network.protocol.NetworkService;
import io.libp2p.core.Host;
import io.libp2p.core.PeerId;
import io.libp2p.core.Stream;

import java.util.Optional;

/**
 * Service used to send messages on {@link Grandpa} protocol.
 */
public class GrandpaService extends NetworkService<Grandpa> {
    ConnectionManager connectionManager = ConnectionManager.getInstance();
    public GrandpaService(String protocolId) {
        this.protocol = new Grandpa(protocolId, new GrandpaProtocol());
    }

    /**
     * Send a neighbour message to peer. If there is no initiator stream opened with peer, send a handshake instead.
     *
     * @param us our host object
     * @param peerId message receiver
     */
    public void sendNeighbourMessage(Host us, PeerId peerId) {
        Optional.ofNullable(connectionManager.getPeerInfo(peerId))
                .map(p -> p.getGrandpaStreams().getInitiator())
                .ifPresentOrElse(
                        this::sendNeighbourMessage,
                        () -> sendHandshake(us, peerId)
                );
    }

    private void sendNeighbourMessage(Stream stream) {
        GrandpaController controller = new GrandpaController(stream);
        controller.sendNeighbourMessage();
    }

    private void sendHandshake(Host us, PeerId peerId) {
        GrandpaController controller = this.protocol.dialPeer(us, peerId, us.getAddressBook());
        controller.sendHandshake();
    }
}
