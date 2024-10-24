package com.limechain.network.protocol.transaction;

import com.limechain.network.ConnectionManager;
import com.limechain.network.protocol.NetworkService;
import io.libp2p.core.Host;
import io.libp2p.core.PeerId;
import io.libp2p.core.Stream;
import lombok.extern.java.Log;

import java.util.Optional;

@Log
public class TransactionsService extends NetworkService<TransactionMessages> {
    ConnectionManager connectionManager = ConnectionManager.getInstance();

    public TransactionsService(String protocolId) {
        this.protocol = new TransactionMessages(protocolId, new TransactionsProtocol());
    }

    /**
     * Sends a transactions message to a peer. If there is no initiator stream opened with the peer,
     * sends a handshake instead.
     *
     * @param us     our host object
     * @param peerId message receiver
     */
    public void sendTransactionsMessage(Host us, PeerId peerId) {
        // TODO Network improvements Keep track of peers that we've notified about each transaction
        Optional.ofNullable(connectionManager.getPeerInfo(peerId))
                .map(p -> p.getTransactionsStreams().getInitiator())
                .ifPresentOrElse(
                        this::sendTransactions,
                        () -> sendHandshake(us, peerId)
                );
    }

    private void sendTransactions(Stream stream) {
        //TODO Send transaction messages
    }

    public void sendHandshake(Host us, PeerId peerId) {
        try {
            TransactionController controller = this.protocol.dialPeer(us, peerId, us.getAddressBook());
            controller.sendHandshake();
        } catch (IllegalStateException e) {
            log.warning("Failed sending transaction handshake to peer " + peerId);
        }
    }
}
