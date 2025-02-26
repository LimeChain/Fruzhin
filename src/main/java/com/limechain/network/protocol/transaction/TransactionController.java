package com.limechain.network.protocol.transaction;

import com.limechain.network.protocol.base.BaseController;
import io.libp2p.core.Stream;

/**
 * A controller for sending message on a Transactions stream.
 */
public class TransactionController extends BaseController<TransactionEngine> {

    public TransactionController(Stream stream) {
        super(stream, new TransactionEngine());
    }

    /**
     * Sends a neighbour message over the controller stream.
     */
    public void sendTransactionsMessage(byte[] encodedTransactionMessage) {
        engine.writeTransactionsMessage(stream, encodedTransactionMessage);
    }
}
