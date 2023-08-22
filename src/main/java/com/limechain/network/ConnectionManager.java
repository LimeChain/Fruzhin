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

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
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
        ProtocolStreams protocolStreams =
                type == ProtocolStreamType.GRANDPA
                        ? peerInfo.getGrandpaStreams()
                        : peerInfo.getBlockAnnounceStreams();

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
        PeerInfo peerInfo = peers.get(stream.remotePeerId());
        if (peerInfo == null) {
            return;
        }

        if (stream.isInitiator()) {
            peerInfo.getGrandpaStreams().setInitiator(null);
        } else {
            peerInfo.getGrandpaStreams().setResponder(null);
        }
    }

    public void closeBlockAnnounceStream(Stream stream) {
        PeerInfo peerInfo = peers.get(stream.remotePeerId());
        if (peerInfo == null) {
            return;
        }

        if (stream.isInitiator()) {
            peerInfo.getBlockAnnounceStreams().setInitiator(null);
        } else {
            peerInfo.getBlockAnnounceStreams().setResponder(null);
        }
    }

    public void updatePeer(PeerId peerId, BlockAnnounceMessage blockAnnounceMessage) {
        PeerInfo peer = peers.get(peerId);
        if (peer == null) {
            return;
        }
        updateLatestBlock(peer, blockAnnounceMessage.getHeader().getBlockNumber());
        if (blockAnnounceMessage.isBestBlock()) {
            updateBestBlock(peer, blockAnnounceMessage.getHeader());
        }
    }

    public void updatePeer(PeerId peerId, BlockAnnounceHandshake blockAnnounceHandshake) {
        PeerInfo peerInfo = peers.get(peerId);
        if (peerInfo == null) {
            return;
        }
        peerInfo.setNodeRole(blockAnnounceHandshake.getNodeRole());
        peerInfo.setGenesisBlockHash(blockAnnounceHandshake.getGenesisBlockHash());
        peerInfo.setBestBlock(blockAnnounceHandshake.getBestBlock());
        peerInfo.setBestBlockHash(blockAnnounceHandshake.getBestBlockHash());
    }

    private void updateLatestBlock(PeerInfo peerInfo, BigInteger announcedBlock) {
        BigInteger latestRecordedBlock = BigInteger.valueOf(peerInfo.getLatestBlock());
        if (announcedBlock.compareTo(latestRecordedBlock) > 0) {
            peerInfo.setLatestBlock(announcedBlock.intValue());
        }
    }

    private void updateBestBlock(PeerInfo peerInfo, BlockHeader blockHeader) {
        peerInfo.setBestBlock(blockHeader.getBlockNumber());
        peerInfo.setBestBlockHash(new Hash256(blockHeader.getHash()));
    }

    public void removePeer(PeerId peerId) {
        peers.remove(peerId);
    }

    public boolean isGrandpaConnected(PeerId peerId) {
        return peers.containsKey(peerId) && peers.get(peerId).getGrandpaStreams() != null;
    }

    public boolean isBlockAnnounceConnected(PeerId peerId) {
        return peers.containsKey(peerId) && peers.get(peerId).getBlockAnnounceStreams() != null;
    }

    public Set<PeerId> getPeerIds(){
        return peers.keySet();
    }
}
