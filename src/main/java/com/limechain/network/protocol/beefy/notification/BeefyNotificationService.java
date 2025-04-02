package com.limechain.network.protocol.beefy.notification;

import com.limechain.network.ConnectionManager;
import com.limechain.network.protocol.NetworkService;
import io.libp2p.core.Host;
import io.libp2p.core.PeerId;
import lombok.extern.java.Log;

import java.util.Optional;

/**
 * Service for sending messages on {@link BeefyNotification} protocol.
 */
@Log
public class BeefyNotificationService extends NetworkService<BeefyNotification> {

    ConnectionManager connectionManager = ConnectionManager.getInstance();

    public BeefyNotificationService(String protocolId) {
        this.protocol = new BeefyNotification(protocolId, new BeefyNotificationProtocol());
    }

    /**
     * Sends a beefy vote message to a peer. If there is no initiator stream opened with the peer,
     * sends a handshake instead.
     *
     * @param us             our host object
     * @param peerId         message receiver
     * @param encodedMessage scale encoded representation of the BeefyVoteMessage object
     */
    public void sendVoteMessage(Host us, PeerId peerId, byte[] encodedMessage) {
        Optional.ofNullable(connectionManager.getPeerInfo(peerId))
                .map(p -> p.getBeefyStreams().getInitiator())
                .ifPresentOrElse(
                        stream -> new BeefyNotificationController(stream).sendVoteMessage(encodedMessage),
                        () -> sendHandshake(us, peerId)
                );
    }

    public void sendHandshake(Host us, PeerId peerId) {
        try {
            BeefyNotificationController controller = this.protocol.dialPeer(us, peerId, us.getAddressBook());
            controller.sendHandshake();
        } catch (Exception e) {
            log.warning("Failed to send Beefy handshake to " + peerId);
        }
    }
}
