package com.limechain.consensus.beefy.dto;

import jakarta.annotation.Nullable;
import lombok.Data;
import lombok.Setter;
import lombok.extern.java.Log;
import org.javatuples.Pair;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

@Data
@Log
public class BeefySession {

    private final BeefyAuthoritySet authoritySet;

    private Map<Commitment, BeefyRound> rounds = new HashMap<>();

    private final BigInteger mandatoryBlock;

    @Setter
    private boolean isMandatoryBlockFinalized;

    @Setter
    @Nullable
    private BigInteger highestFinalized;

    @Nullable
    private final Pair<byte[], byte[]> beefyKeyPair;

    public void update(BigInteger blockNumber) {
        // remove rounds <= block number(round number)
        rounds.keySet().removeIf(commitment ->
                commitment.getBlockNumber().compareTo(blockNumber) <= 0
        );

        highestFinalized = (highestFinalized == null) ? blockNumber : highestFinalized.max(blockNumber);

        if (blockNumber.equals(mandatoryBlock)) {
            isMandatoryBlockFinalized = true;
            log.fine(String.format("finalizeJustification: Finalize mandatory round: %d.", blockNumber));
        } else {
            log.fine(String.format("finalizeJustification: Finalize non-mandatory round: %d.", blockNumber));
        }
    }
}
