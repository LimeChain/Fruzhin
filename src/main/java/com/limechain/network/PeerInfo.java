package com.limechain.network;

import com.limechain.network.protocol.blockannounce.scale.BlockAnnounceHandshake;
import io.emeraldpay.polkaj.types.Hash256;
import lombok.Data;

@Data
public class PeerInfo {
    private int nodeRole;
    private String bestBlock;
    private Hash256 bestBlockHash;
    private Hash256 genesisBlockHash;

    public PeerInfo(BlockAnnounceHandshake handshake) {
        nodeRole = handshake.getNodeRole();
        bestBlock = handshake.getBestBlock();
        bestBlockHash = handshake.getBestBlockHash();
        genesisBlockHash = handshake.getGenesisBlockHash();
    }
}
