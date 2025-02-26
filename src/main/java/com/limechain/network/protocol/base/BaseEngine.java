package com.limechain.network.protocol.base;

import io.libp2p.core.PeerId;
import io.libp2p.core.Stream;

/**
 * Abstract engine for handling transactions on specific streams
 */
public abstract class BaseEngine {

    protected abstract void handleHandshake(byte[] message, PeerId peerId, Stream stream);

    public abstract void receiveRequest(byte[] message, Stream stream);

    public abstract void writeHandshakeToStream(Stream stream, PeerId peerId);
}