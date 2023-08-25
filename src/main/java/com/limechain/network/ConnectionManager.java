package com.limechain.network;

import com.limechain.network.dto.PeerInfo;
import com.limechain.network.dto.ProtocolStreamType;
import com.limechain.network.dto.ProtocolStreams;
import com.limechain.network.protocol.blockannounce.scale.BlockAnnounceHandshake;
import com.limechain.network.protocol.blockannounce.scale.BlockAnnounceMessage;
import com.limechain.network.protocol.warp.dto.BlockHeader;
import io.emeraldpay.polkaj.types.Hash256;
import io.libp2p.core.PeerId;
import io.libp2p.core.Stream;
import lombok.extern.java.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;

@Log
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

    public PeerInfo getPeerInfo(PeerId peerId) {
       return peers.get(peerId);
    }

    public void addBlockAnnounceStream(Stream stream) {
        addStream(stream, ProtocolStreamType.BLOCK_ANNOUNCE);
    }

    public void addGrandpaStream(Stream stream) {
        addStream(stream, ProtocolStreamType.GRANDPA);
    }

    private void addStream(Stream stream, ProtocolStreamType type) {
        PeerId peerId = stream.remotePeerId();
        PeerInfo peerInfo = Optional.ofNullable(peers.get(peerId)).orElseGet(() -> addNewPeer(peerId));
        ProtocolStreams protocolStreams = peerInfo.getProtocolStreams(type);

        if (stream.isInitiator()) {
            if (protocolStreams.getInitiator() != null) {
                stream.close();
                return;
            }
            protocolStreams.setInitiator(stream);
        } else {
            if (protocolStreams.getResponder() != null) {
                stream.close();
                return;
            }
            protocolStreams.setResponder(stream);
        }
    }

    private PeerInfo addNewPeer(PeerId peerId) {
        PeerInfo peerInfo = new PeerInfo();
        peers.put(peerId, peerInfo);
        return peerInfo;
    }

    public void closeGrandpaStream(Stream stream) {
        closeStream(stream, ProtocolStreamType.GRANDPA);
    }

    public void closeBlockAnnounceStream(Stream stream) {
        closeStream(stream, ProtocolStreamType.BLOCK_ANNOUNCE);
    }

    private void closeStream(Stream stream, ProtocolStreamType type) {
        PeerInfo peerInfo = peers.get(stream.remotePeerId());
        if (peerInfo == null) {
            log.log(Level.WARNING, "Trying to close a missing stream for peer " + stream.remotePeerId());
            return;
        }

        ProtocolStreams protocolStreams = peerInfo.getProtocolStreams(type);

        if (stream.isInitiator()) {
            protocolStreams.setInitiator(null);
        } else {
            protocolStreams.setResponder(null);
        }
    }

    public void updatePeer(PeerId peerId, BlockAnnounceHandshake blockAnnounceHandshake) {
        PeerInfo peerInfo = peers.get(peerId);
        if (peerInfo == null) {
            log.log(Level.WARNING, "Trying to update missing peer " + peerId);
            return;
        }
        peerInfo.setNodeRole(blockAnnounceHandshake.getNodeRole());
        peerInfo.setGenesisBlockHash(blockAnnounceHandshake.getGenesisBlockHash());
        peerInfo.setBestBlock(blockAnnounceHandshake.getBestBlock());
        peerInfo.setBestBlockHash(blockAnnounceHandshake.getBestBlockHash());
    }

    public void updatePeer(PeerId peerId, BlockAnnounceMessage blockAnnounceMessage) {
        PeerInfo peerInfo = peers.get(peerId);
        if (peerInfo == null) {
            log.log(Level.WARNING, "Trying to update missing peer " + peerId);
            return;
        }

        BlockHeader blockHeader = blockAnnounceMessage.getHeader();
        if (blockHeader.getBlockNumber().compareTo(peerInfo.getLatestBlock()) > 0) {
            peerInfo.setLatestBlock(blockHeader.getBlockNumber());
        }
        if (blockAnnounceMessage.isBestBlock()) {
            peerInfo.setBestBlock(blockHeader.getBlockNumber());
            peerInfo.setBestBlockHash(new Hash256(blockHeader.getHash()));
        }
    }

    public void removePeer(PeerId peerId) {
        peers.remove(peerId);
    }

    public boolean isGrandpaConnected(PeerId peerId) {
        return peers.containsKey(peerId) && peers.get(peerId).getGrandpaStreams().getResponder() != null;
    }

    public boolean isBlockAnnounceConnected(PeerId peerId) {
        return peers.containsKey(peerId) && peers.get(peerId).getBlockAnnounceStreams().getResponder() != null;
    }

    public Set<PeerId> getPeerIds(){
        return peers.keySet();
    }
}
