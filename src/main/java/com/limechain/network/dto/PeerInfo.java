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
    private int latestBlock;
    private final ProtocolStreams blockAnnounceStreams = new ProtocolStreams();
    private final ProtocolStreams grandpaStreams = new ProtocolStreams();

    public boolean isGrandpaConnected() {
        return grandpaStreams.getResponder() != null;
    }

    public boolean isBlockAnnounceConnected() {
        return blockAnnounceStreams.getResponder() != null;
    }
}
