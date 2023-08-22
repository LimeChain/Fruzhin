package com.limechain.network.protocol.grandpa;

import com.limechain.network.StrictProtocolBinding;

public class Grandpa extends StrictProtocolBinding<GrandpaController> {
    public Grandpa(String protocolId, GrandpaProtocol protocol) {
        super(protocolId, protocol);
    }
}
