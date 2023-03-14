package com.limechain.rpc.subscriptions.chainhead;

public interface ChainHeadRpc {
    void chainUnstableFollow(boolean runtimeUpdates);

    void chainUnstableUnfollow(String subscriptionId);

    void chainUnstableUnpin();

    void chainUnstableStorage();

    void chainUnstableCall();

    void chainUnstableStopCall();
}
