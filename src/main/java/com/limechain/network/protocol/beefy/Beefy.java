package com.limechain.network.protocol.beefy;

import com.limechain.network.StrictProtocolBinding;

/**
 * BEEFY protocol binding
 */
public class Beefy extends StrictProtocolBinding<BeefyController> {
    public Beefy(String protocolId, BeefyProtocol protocol) {
        super(protocolId, protocol);
    }
}
