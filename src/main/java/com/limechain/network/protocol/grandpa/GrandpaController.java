package com.limechain.network.protocol.grandpa;

import io.libp2p.core.Stream;

public class GrandpaController {
    protected final GrandpaEngine engine = GrandpaEngine.getInstance();
    protected final Stream stream;

    public GrandpaController(Stream stream) {
        this.stream = stream;
    }

    public void sendHandshake() {
        engine.writeHandshakeToStream(stream, stream.remotePeerId());
    }

    public void sendNeighbourMessage() {
        engine.writeNeighbourMessage(stream, stream.remotePeerId());
    }
}
