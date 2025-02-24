package com.limechain.network.protocol.beefy;

import io.libp2p.core.Stream;

/**
 * A controller for sending message on a BEEFY stream
 */
public class BeefyController {

    protected final Stream stream;
    protected BeefyEngine engine = new BeefyEngine();

    public BeefyController(Stream stream) {
        this.stream = stream;
    }

    /**
     * Sends a handshake message over the controller stream.
     */
    public void sendHandshake() {
        engine.writeHandshakeToStream(stream, stream.remotePeerId());
    }

    public void sendVoteMessage() {
        //TODO
    }
}
