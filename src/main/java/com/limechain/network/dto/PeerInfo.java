package com.limechain.network.dto;

import com.limechain.polkaj.Hash256;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;

@Data
@NoArgsConstructor
public class PeerInfo {
//    private PeerId peerId;
    private int nodeRole;
    private BigInteger bestBlock;
    private Hash256 bestBlockHash;
    private Hash256 genesisBlockHash;
    private BigInteger latestBlock = BigInteger.ZERO;
    private final ProtocolStreams blockAnnounceStreams = new ProtocolStreams();
    private final ProtocolStreams grandpaStreams = new ProtocolStreams();
    private final ProtocolStreams transactionsStreams = new ProtocolStreams();

    public String getNodeRoleName(){
//        return Arrays
//                .stream(NodeRole.values())
//                .filter(role -> role.getValue() == nodeRole)
//                .findFirst()
//                .orElse(NodeRole.NONE)
//                .name();
        return null;
    }

    public ProtocolStreams getProtocolStreams(ProtocolStreamType type) {
        return switch (type) {
            case GRANDPA -> grandpaStreams;
            case BLOCK_ANNOUNCE -> blockAnnounceStreams;
            case TRANSACTIONS -> transactionsStreams;
        };
    }

}
