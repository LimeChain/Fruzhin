package com.limechain.network.protocol.beefy.notification;

import com.limechain.network.StrictProtocolBinding;

/**
 * BEEFY notification protocol binding
 */
public class BeefyNotification extends StrictProtocolBinding<BeefyNotificationController> {
    public BeefyNotification(String protocolId, BeefyNotificationProtocol protocol) {
        super(protocolId, protocol);
    }
}
