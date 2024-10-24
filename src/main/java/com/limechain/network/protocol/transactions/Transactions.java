package com.limechain.network.protocol.transactions;

import com.limechain.network.StrictProtocolBinding;

/**
 * Transactions protocol binding
 */
public class Transactions extends StrictProtocolBinding<TransactionsController> {
    public Transactions(String protocolId, TransactionsProtocol protocol) {
        super(protocolId, protocol);
    }
}
