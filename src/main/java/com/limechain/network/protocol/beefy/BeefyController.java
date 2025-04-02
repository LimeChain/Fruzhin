package com.limechain.network.protocol.beefy;

import com.limechain.network.protocol.base.BaseController;
import io.libp2p.core.Stream;

/**
 * A controller for sending message on a BEEFY stream
 */
public class BeefyController extends BaseController<BeefyEngine> {

    public BeefyController(Stream stream) {
        super(stream, new BeefyEngine());
    }

    /**
     * Sends a beefy vote message over the controller stream
     */
    public void sendVoteMessage(byte[] encodedBeefyVoteMessage) {
        engine.writeBeefyVoteMessage(stream, encodedBeefyVoteMessage);
    }
}
