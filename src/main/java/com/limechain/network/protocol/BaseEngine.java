package com.limechain.network.protocol;

import io.libp2p.core.PeerId;
import io.libp2p.core.Stream;

public abstract class BaseEngine {

    protected abstract void handleHandshake(byte[] message, PeerId peerId, Stream stream);

    public abstract void receiveRequest(byte[] message, Stream stream);

    public abstract void writeHandshakeToStream(Stream stream, PeerId peerId);
}