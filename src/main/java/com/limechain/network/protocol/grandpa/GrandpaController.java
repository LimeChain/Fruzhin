package com.limechain.network.protocol.grandpa;

import io.libp2p.core.Stream;

/**
 * A controller for sending message on a GRANDPA stream.
 */
public class GrandpaController {
    protected final GrandpaEngine engine = new GrandpaEngine();
    protected final Stream stream;

    public GrandpaController(Stream stream) {
        this.stream = stream;
    }

    /**
     * Send a handshake message on the controller stream.
     */
    public void sendHandshake() {
        engine.writeHandshakeToStream(stream, stream.remotePeerId());
    }

    /**
     * Send a neighbour message on the controller stream.
     */
    public void sendNeighbourMessage() {
        engine.writeNeighbourMessage(stream, stream.remotePeerId());
    }
}
