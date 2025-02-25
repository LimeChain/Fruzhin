package com.limechain.network.protocol.beefy;

import com.limechain.network.protocol.BaseController;
import io.libp2p.core.Stream;

/**
 * A controller for sending message on a BEEFY stream
 */
public class BeefyController extends BaseController<BeefyEngine> {

    public BeefyController(Stream stream) {
        super(stream, new BeefyEngine());
    }

    public void sendVoteMessage() {
        //TODO
    }
}
