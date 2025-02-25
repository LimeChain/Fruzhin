package com.limechain.network.protocol;

import com.limechain.network.ConnectionManager;
import io.libp2p.core.PeerId;
import io.libp2p.core.Stream;

public abstract class BaseEngine {

    protected final ConnectionManager connectionManager = ConnectionManager.getInstance();

    protected abstract void handleHandshake(byte[] message, PeerId peerId, Stream stream);

    public abstract void receiveRequest(byte[] message, Stream stream);

    public abstract void writeHandshakeToStream(Stream stream, PeerId peerId);
}
