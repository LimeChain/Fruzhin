package com.limechain.network.protocol.transactions;

import com.limechain.network.ConnectionManager;
import com.limechain.network.protocol.NetworkService;
import io.libp2p.core.Host;
import io.libp2p.core.PeerId;
import io.libp2p.core.Stream;

import java.util.Optional;

public class TransactionsService extends NetworkService<Transactions> {
    ConnectionManager connectionManager = ConnectionManager.getInstance();
    public TransactionsService(String protocolId) {
        this.protocol = new Transactions(protocolId, new TransactionsProtocol());
    }

    /**
     * Sends a transactions message to a peer. If there is no initiator stream opened with the peer,
     * sends a handshake instead.
     *
     * @param us our host object
     * @param peerId message receiver
     */
    public void sendTransactionsMessage(Host us, PeerId peerId) {
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

    private void sendHandshake(Host us, PeerId peerId) {
        TransactionsController controller = this.protocol.dialPeer(us, peerId, us.getAddressBook());
        controller.sendHandshake();
    }
}
