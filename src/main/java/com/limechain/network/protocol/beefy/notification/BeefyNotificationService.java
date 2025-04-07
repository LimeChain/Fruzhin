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
     * Sends a BEEFY message to a peer.
     * <p>
     * Sends both vote messages and signed commitments, as both are transmitted over
     * the same initiator stream. If there is no initiator stream opened with the peer,
     * sends a handshake instead.
     * </p>
     * @param us our host object.
     * @param peerId the message receiver.
     * @param encodedMessage a scale encoded representation of either a BeefyVoteMessage or a SignedCommitment.
     */
    public void sendMessage(Host us, PeerId peerId, byte[] encodedMessage) {
        Optional.ofNullable(connectionManager.getPeerInfo(peerId))
                .map(p -> p.getBeefyStreams().getInitiator())
                .ifPresentOrElse(
                        stream -> new BeefyNotificationController(stream).sendMessage(encodedMessage),
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
