package com.limechain.network;

import io.libp2p.core.PeerId;

import java.util.HashSet;
import java.util.Set;

public class ConnectionManager {
    private static ConnectionManager INSTANCE;
    protected final Set<PeerId> peers = new HashSet<>();

    protected ConnectionManager() {}

    public static ConnectionManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ConnectionManager();
        }
        return INSTANCE;
    }

    public void addPeer(PeerId peerId) {
        peers.add(peerId);
    }

    public void removePeer(PeerId peerId) {
        peers.remove(peerId);
    }

    public boolean isConnected(PeerId peerId) {
        return peers.contains(peerId);
    }

    public Set<PeerId> getPeerIds(){
        return peers;
    }
}
