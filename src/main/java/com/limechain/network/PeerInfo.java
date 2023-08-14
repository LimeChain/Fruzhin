package com.limechain.network;

import com.limechain.network.protocol.blockannounce.scale.BlockAnnounceHandshake;
import io.emeraldpay.polkaj.types.Hash256;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PeerInfo {
    private int nodeRole;
    private String bestBlock;
    private Hash256 bestBlockHash;
    private Hash256 genesisBlockHash;
    private int latestBlock;
    private boolean blockAnnounceConnected;
    private boolean grandpaConnected;

    public PeerInfo(BlockAnnounceHandshake handshake) {
        nodeRole = handshake.getNodeRole();
        bestBlock = handshake.getBestBlock();
        bestBlockHash = handshake.getBestBlockHash();
        genesisBlockHash = handshake.getGenesisBlockHash();
        blockAnnounceConnected = true;
    }
}
