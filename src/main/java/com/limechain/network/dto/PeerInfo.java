package com.limechain.network.dto;

import com.limechain.network.protocol.blockannounce.NodeRole;
import io.emeraldpay.polkaj.types.Hash256;
import io.libp2p.core.PeerId;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;
import java.util.Arrays;

@Data
@NoArgsConstructor
public class PeerInfo {

    private final ProtocolStreams transactionsStreams = new ProtocolStreams();
    private final ProtocolStreams blockAnnounceStreams = new ProtocolStreams();
    private final ProtocolStreams grandpaStreams = new ProtocolStreams();
    private final ProtocolStreams beefyStreams = new ProtocolStreams();

    private PeerId peerId;
    private int nodeRole;
    private BigInteger bestBlock;
    private Hash256 bestBlockHash;
    private Hash256 genesisBlockHash;
    private BigInteger latestBlock = BigInteger.ZERO;

    private BigInteger setId;
    private BigInteger roundNumber;
    private BigInteger lastFinalizedBlock;

    public String getNodeRoleName() {
        return Arrays
                .stream(NodeRole.values())
                .filter(role -> role.getValue() == nodeRole)
                .findFirst()
                .orElse(NodeRole.NONE)
                .name();
    }

    public ProtocolStreams getProtocolStreams(ProtocolStreamType type) {
        return switch (type) {
            case TRANSACTIONS -> transactionsStreams;
            case BLOCK_ANNOUNCE -> blockAnnounceStreams;
            case GRANDPA -> grandpaStreams;
            case BEEFY -> beefyStreams;
        };
    }
}
