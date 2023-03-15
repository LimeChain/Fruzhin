package com.limechain.rpc.subscriptions.chainhead;

public interface ChainHeadRpc {
    void chainUnstableFollow(boolean runtimeUpdates);

    void chainUnstableUnfollow(String subscriptionId);

    void chainUnstableUnpin(String subscriptionId, String blockHash);

    void chainUnstableCall(String subscriptionId, String blockHash, String function, String callParameters);

    void chainUnstableStorage(String subscriptionId, String blockHash, String key);

    void chainUnstableStopCall(String subscriptionId);
}
