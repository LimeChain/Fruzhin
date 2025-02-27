package com.limechain.network.protocol.beefy;

import com.limechain.network.ConnectionManager;
import com.limechain.network.protocol.NetworkService;
import io.libp2p.core.Host;
import io.libp2p.core.PeerId;
import lombok.extern.java.Log;

/**
 * Service for sending messages on {@link Beefy} protocol.
 */
@Log
public class BeefyService extends NetworkService<Beefy> {

    ConnectionManager connectionManager = ConnectionManager.getInstance();

    public BeefyService(String protocolId) {
        this.protocol = new Beefy(protocolId, new BeefyProtocol());
    }

    //TODO: write doc
    public void sendVoteMessage(Host us, PeerId peerId, byte[] encodedMessage) {
        //TODO
    }

    public void sendHandshake(Host us, PeerId peerId) {
        try {
            BeefyController controller = this.protocol.dialPeer(us, peerId, us.getAddressBook());
            controller.sendHandshake();
        } catch (Exception e) {
            log.warning("Failed to send Beefy handshake to " + peerId);
        }
    }
}
