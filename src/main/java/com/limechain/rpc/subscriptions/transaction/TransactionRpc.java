package com.limechain.rpc.subscriptions.transaction;

/**
 * Interface for transaction rpc methods family that the light client <b>must</b> implement
 */
public interface TransactionRpc {
    /**
     * Submits a transaction for inclusion in the chain.
     *
     * @param transaction String containing the hexadecimal-encoded
     *                    SCALE-encoded transaction to try to include in a block.
     * @see <a href="https://paritytech.github.io/json-rpc-interface-spec/api/transaction_unstable_submitAndWatch.html">
     * transaction_unstable_submitAndWatch</a>
     */
    void transactionUnstableSubmitAndWatch(String transaction);

    /**
     * Removes subscription for transaction inclusion result from {@link #transactionUnstableSubmitAndWatch(String)}
     *
     * @param subscriptionId Opaque string equal to the value returned by
     *                       {@link  #transactionUnstableSubmitAndWatch(String)}
     */
    void transactionUnstableUnwatch(String subscriptionId);
}
