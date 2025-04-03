package com.limechain.network.protocol.beefy.requestresponse;

import com.limechain.network.protocol.NetworkService;

public class BeefyJustificationService extends NetworkService<BeefyJustificationMessages> {
    public BeefyJustificationService(String protocolId) {
        this.protocol = new BeefyJustificationMessages(protocolId, new BeefyJustificationProtocol());
    }
}
