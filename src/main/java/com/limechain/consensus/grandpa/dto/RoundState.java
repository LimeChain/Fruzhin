package com.limechain.consensus.grandpa.dto;

import com.limechain.network.protocol.warp.dto.BlockHeader;
import lombok.Builder;
import lombok.Value;

import java.math.BigInteger;

@Value
@Builder
public class RoundState {

    BlockHeader lastFinalizedBlock;
    BlockHeader finalizedBlock;
    BigInteger roundNumber;
    GrandpaAuthoritySet authoritySet;
}
