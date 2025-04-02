package com.limechain.network.protocol.beefy.notification;

import com.limechain.network.protocol.base.BaseController;
import io.libp2p.core.Stream;

/**
 * A controller for sending message on a BEEFY notification stream
 */
public class BeefyNotificationController extends BaseController<BeefyNotificationEngine> {

    public BeefyNotificationController(Stream stream) {
        super(stream, new BeefyNotificationEngine());
    }

    /**
     * Sends a beefy vote message over the controller stream
     */
    public void sendVoteMessage(byte[] encodedBeefyVoteMessage) {
        engine.writeBeefyVoteMessage(stream, encodedBeefyVoteMessage);
    }
}
