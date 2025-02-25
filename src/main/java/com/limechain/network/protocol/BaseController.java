package com.limechain.network.protocol;

import io.libp2p.core.Stream;

/**
 * An abstract controller for sending message on a specific stream
 */
public abstract class BaseController<E extends BaseEngine> {

    protected final Stream stream;
    protected final E engine;

    protected BaseController(Stream stream, E engine) {
        this.stream = stream;
        this.engine = engine;
    }

    /**
     * Sends a handshake message over the controller stream.
     */
    public void sendHandshake() {
        engine.writeHandshakeToStream(stream, stream.remotePeerId());
    }
}
