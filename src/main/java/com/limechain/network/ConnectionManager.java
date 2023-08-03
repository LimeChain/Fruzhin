package com.limechain.network;

import com.limechain.network.protocol.blockannounce.scale.BlockAnnounceMessage;
import com.limechain.network.protocol.warp.dto.BlockHeader;
import io.emeraldpay.polkaj.types.Hash256;
import io.libp2p.core.PeerId;

import java.math.BigInteger;
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

    public PeerInfo getPeerInfo(PeerId peerId) {
       return peers.get(peerId);
    }

    public void updatePeer(PeerId peerId, BlockAnnounceMessage blockAnnounceMessage) {
        final var peer = peers.get(peerId);
        updateLatestBlock(peer, blockAnnounceMessage.getHeader().getBlockNumber());

        if(blockAnnounceMessage.isBestBlock()) {
            updateBestBlock(peer, blockAnnounceMessage.getHeader());
        }
    }

    // TODO: decide if needed
    private void updateLatestBlock(PeerInfo peerInfo, BigInteger announcedBlock) {
        BigInteger latestRecordedBlock = BigInteger.valueOf(peerInfo.getLatestBlock());
        if(announcedBlock.compareTo(latestRecordedBlock) > 0) {
            peerInfo.setLatestBlock(announcedBlock.intValue());
        }
    }

    private void updateBestBlock(PeerInfo peerInfo, BlockHeader blockHeader) {
        peerInfo.setBestBlock(blockHeader.getBlockNumber().toString());
        peerInfo.setBestBlockHash(new Hash256(blockHeader.getHash()));
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
