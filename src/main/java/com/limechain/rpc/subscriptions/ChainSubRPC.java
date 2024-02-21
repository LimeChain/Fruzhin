package com.limechain.rpc.subscriptions;

public interface ChainSubRPC {
    String chainSubscribeAllHeads();

    String chainUnsubscribeAllHeads();

    String chainSubscribeNewHeads();

    String chainUnsubscribeNewHeads();

    String chainSubscribeFinalizedHeads();

    String chainUnsubscribeFinalizedHeads();
}
