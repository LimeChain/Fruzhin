package com.limechain.network.dto;

import io.emeraldpay.polkaj.types.Hash256;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;

@Data
@NoArgsConstructor
public class PeerInfo {
    private int nodeRole;
    private BigInteger bestBlock;
    private Hash256 bestBlockHash;
    private Hash256 genesisBlockHash;
    private BigInteger latestBlock;
    private final ProtocolStreams blockAnnounceStreams = new ProtocolStreams();
    private final ProtocolStreams grandpaStreams = new ProtocolStreams();

    public ProtocolStreams getProtocolStreams(ProtocolStreamType type) {
        return switch (type) {
            case GRANDPA -> grandpaStreams;
            case BLOCK_ANNOUNCE -> blockAnnounceStreams;
        };
    }
}
