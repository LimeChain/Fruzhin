package com.limechain.rpc.subscriptions.transaction;

public interface TransactionRpc {
    void transactionUnstableSubmitAndWatch(String transaction);

    void transactionUnstableWatch(String subscriptionId);
}
