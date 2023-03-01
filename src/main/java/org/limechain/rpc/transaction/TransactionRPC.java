package org.limechain.rpc.transaction;

import com.googlecode.jsonrpc4j.JsonRpcMethod;

public interface TransactionRPC {
    @JsonRpcMethod("transaction_unstable_submitAndWatch")
    String transactionUnstableSubmitAndWatch ();
}
