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
     * Sends a BEEFY message over the controller stream.
     * <p>
     * Since both vote messages and signed commitments are transmitted over the same stream,
     * there is no need for separate logic after the message is encoded.
     * </p>
     * @param encodedMessage the encoded BEEFY message to send, which can be either a vote message or a signed commitment.
     */
    public void sendMessage(byte[] encodedMessage) {
        engine.writeMessage(stream, encodedMessage);
    }
}
