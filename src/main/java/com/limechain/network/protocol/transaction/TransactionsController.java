package com.limechain.network.protocol.transaction;

import io.libp2p.core.Stream;

/**
 * A controller for sending message on a Transactions stream.
 */
public class TransactionsController {
    protected final TransactionsEngine engine = new TransactionsEngine();
    protected final Stream stream;

    public TransactionsController(Stream stream) {
        this.stream = stream;
    }

    /**
     * Sends a handshake message over the controller stream.
     */
    public void sendHandshake() {
        engine.writeHandshakeToStream(stream, stream.remotePeerId());
    }

    /**
     * Sends a neighbour message over the controller stream.
     */
    public void sendTransactionsMessage() {
        engine.writeTransactionsMessage(stream, stream.remotePeerId());
    }
}
