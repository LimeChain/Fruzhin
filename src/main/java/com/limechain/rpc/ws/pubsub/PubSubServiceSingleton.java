package com.limechain.rpc.ws.pubsub;

public class PubSubServiceSingleton {
    private static final PubSubService INSTANCE = new PubSubService();

    // private constructor to avoid client applications using the constructor
    private PubSubServiceSingleton() {
    }

    public static PubSubService getInstance() {
        return INSTANCE;
    }
}
