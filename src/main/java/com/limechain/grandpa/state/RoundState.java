package com.limechain.grandpa.state;

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
    AuthoritySet authoritySet;
}
