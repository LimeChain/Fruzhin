package com.limechain.network;

import com.limechain.network.protocol.blockannounce.scale.BlockAnnounceMessage;
import io.emeraldpay.polkaj.types.Hash256;
import io.libp2p.core.PeerId;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ConnectionManager {
    private static ConnectionManager INSTANCE;
    protected final Map<PeerId, PeerInfo> peers = new HashMap<>();

    protected ConnectionManager() {}

    public static ConnectionManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new ConnectionManager();
        }
        return INSTANCE;
    }

    public void addPeer(PeerId peerId, PeerInfo peerInfo) {
        peers.put(peerId, peerInfo);
    }

    public void updatePeer(PeerId peerId, BlockAnnounceMessage blockAnnounceMessage) {
        if (!blockAnnounceMessage.isBestBlock()) {
            return;
        }

        final var peer = peers.get(peerId);
        peer.setBestBlock(blockAnnounceMessage.getHeader().getBlockNumber().toString());
        peer.setBestBlockHash(new Hash256(blockAnnounceMessage.getHeader().getHash()));
    }

    public void removePeer(PeerId peerId) {
        peers.remove(peerId);
    }

    public boolean isConnected(PeerId peerId) {
        return peers.containsKey(peerId);
    }

    public Set<PeerId> getPeerIds(){
        return peers.keySet();
    }
}
