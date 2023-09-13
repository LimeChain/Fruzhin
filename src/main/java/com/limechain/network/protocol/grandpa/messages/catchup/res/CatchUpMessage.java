package com.limechain.network.protocol.grandpa.messages.catchup.res;

import io.emeraldpay.polkaj.types.Hash256;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CatchUpMessage {
    private BigInteger round;
    private BigInteger setId;
    private SignedVote[] preVotes;
    private SignedVote[] preCommits;
    private Hash256 blockHash;
    private BigInteger blockNumber;
}
