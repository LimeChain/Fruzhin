package com.limechain.network.dto;

import io.emeraldpay.polkaj.types.Hash256;
import io.libp2p.core.PeerId;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;

@Data
@NoArgsConstructor
public class PeerInfo {
    private PeerId peerId;
    private int nodeRole;
    private BigInteger bestBlock;
    private Hash256 bestBlockHash;
    private Hash256 genesisBlockHash;
    private BigInteger latestBlock = BigInteger.ZERO;
    private final ProtocolStreams blockAnnounceStreams = new ProtocolStreams();
    private final ProtocolStreams grandpaStreams = new ProtocolStreams();

    public String getNodeRoleName(){
        return switch (nodeRole) {
            case 1 -> "FULL";
            case 2 -> "LIGHT";
            case 4 -> "AUTHORITY";
            default -> "NONE";
        };
    }

    public ProtocolStreams getProtocolStreams(ProtocolStreamType type) {
        return switch (type) {
            case GRANDPA -> grandpaStreams;
            case BLOCK_ANNOUNCE -> blockAnnounceStreams;
        };
    }

}
