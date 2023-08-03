package com.limechain.network.protocol.grandpa;

import com.limechain.network.protocol.NetworkService;

public class GrandpaService extends NetworkService<Grandpa> {
    public GrandpaService(String protocolId) {
        this.protocol = new Grandpa(protocolId, new GrandpaProtocol());
    }
}
