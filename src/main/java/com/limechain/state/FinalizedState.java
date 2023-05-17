package com.limechain.state;

import com.limechain.chain.lightsyncstate.Authority;
import com.limechain.chain.lightsyncstate.BabeEpoch;
import com.limechain.network.protocol.warp.dto.BlockHeader;

import java.math.BigInteger;

public class FinalizedState {
    public BlockHeader finalizedBlockHeader;
    public BabeEpoch finalizedBlockEpochInformation;
    public BabeEpoch finalizedBlockEpochTransition;
    public int authoritySetId;
    public Authority[] nextAuthorities;

    public void validate() {
        this.finalizedBlockEpochTransition.validate();
        if (this.finalizedBlockEpochTransition.getSlotNumber().compareTo(BigInteger.ZERO) > 0
                && this.finalizedBlockEpochTransition.getEpochIndex().equals(BigInteger.ZERO)) {
            throw new IllegalStateException("Found a Babe slot start number for future Babe epoch number 0. " +
                    "A future Babe epoch 0 has no known starting slot");
        }

        if (this.finalizedBlockEpochTransition.getSlotNumber().equals(BigInteger.ZERO) &&
                this.finalizedBlockEpochTransition.getEpochIndex().compareTo(BigInteger.ZERO) > 0) {
            throw new IllegalStateException(
                    "Missing Babe slot start number for Babe epoch number other than future epoch 0");
        }

        this.finalizedBlockEpochInformation.validate();

        if (this.finalizedBlockEpochInformation != null) {
            this.finalizedBlockEpochTransition.validate();
            if (this.finalizedBlockHeader.getBlockNumber().equals(BigInteger.ZERO)) {
                throw new IllegalStateException("Finalized block is block number 0, and a Babe epoch " +
                        "information has been provided. This would imply the existence of a block -1 and below.");
            }
            if (this.finalizedBlockEpochInformation.getSlotNumber().equals(BigInteger.ZERO)) {
                throw new IllegalStateException("Missing Babe slot start number for Babe epoch number other than future epoch 0.");
            }
            if (this.finalizedBlockEpochInformation.getEpochIndex()
                    .add(BigInteger.ONE)
                    .compareTo(this.finalizedBlockEpochTransition.getEpochIndex()) != 0) {
                throw new IllegalStateException(
                        "Next Babe epoch number does not immediately follow current Babe epoch number");
            }
        } else if (this.finalizedBlockHeader.getBlockNumber().equals(BigInteger.ZERO)) {
            throw new IllegalStateException(
                    "Finalized block is not number 0, but no Babe epoch information has been provided");
        }
    }
}
