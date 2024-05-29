package com.limechain.network.protocol.state;

import com.limechain.network.protocol.NetworkService;

public class StateService extends NetworkService<StateMessages> {
    public StateService(String protocolId) {
        this.protocol = new StateMessages(protocolId, new StateProtocol());
    }
}
