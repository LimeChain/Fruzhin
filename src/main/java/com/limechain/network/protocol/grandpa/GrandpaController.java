package com.limechain.network.protocol.grandpa;

public interface GrandpaController {
    void sendHandshake();

    void sendNeighbourMessage();

}
