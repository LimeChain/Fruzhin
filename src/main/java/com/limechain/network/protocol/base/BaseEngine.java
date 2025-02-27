package com.limechain.network.protocol.base;

import io.libp2p.core.PeerId;
import io.libp2p.core.Stream;

/**
 * Abstract engine for handling transactions on specific streams
 */
public interface BaseEngine {

    void handleHandshake(byte[] message, PeerId peerId, Stream stream);

    void receiveRequest(byte[] message, Stream stream);

    void writeHandshakeToStream(Stream stream, PeerId peerId);
}