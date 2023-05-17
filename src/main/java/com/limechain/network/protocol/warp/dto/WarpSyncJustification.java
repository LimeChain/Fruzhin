package com.limechain.network.protocol.warp.dto;

import io.emeraldpay.polkaj.types.Hash256;
import io.emeraldpay.polkaj.types.Hash512;
import lombok.Setter;
import lombok.extern.java.Log;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

@Setter
@Log
public class WarpSyncJustification {
    public BigInteger round;
    public Hash256 targetHash;
    public BigInteger targetBlock;
    public Precommit[] precommits;
    public BlockHeader[] ancestryVotes;

    @Override
    public String toString() {
        return "WarpSyncJustification{" +
                "round=" + round +
                ", targetHash=" + targetHash +
                ", targetBlock=" + targetBlock +
                ", precommits=" + Arrays.toString(precommits) +
                ", ancestryVotes=" + Arrays.toString(ancestryVotes) +
                '}';
    }

    public boolean validateCommit(Set<Hash256> voters) {
        int invalidVotersCount = 0;
        List<Hash256> validVoters = new ArrayList<>();
        List<Precommit> validPrecommits = new ArrayList<>();

        for (Precommit precommit : precommits) {
            if (!voters.contains(precommit.getAuthorityPublicKey())) {
                invalidVotersCount++;
            } else {
                validVoters.add(precommit.getAuthorityPublicKey());
                validPrecommits.add(precommit);
            }
        }

        var baseTargetHash = Hash256.empty();
        BigInteger baseTargetNumber = BigInteger.ZERO;
        for (Precommit precommit : validPrecommits) {
            if (!precommit.getSignature().equals(Hash512.empty())
                    && precommit.getTargetNumber().compareTo(baseTargetNumber) == -1) {
                    baseTargetHash = precommit.getTargetHash();
                    baseTargetNumber = precommit.getTargetNumber();
            }
        }

        for(Precommit precommit: validPrecommits){
            if(!checkIfPrecommitIsEqualOrDescendent(baseTargetHash, precommit.getTargetHash())){
                log.log(Level.WARNING, "Precommit block not descendent of target");
                return false;
            };
        }

        for(Precommit precommit: validPrecommits){

        }
        return true;
    }

    public boolean checkIfPrecommitIsEqualOrDescendent(Hash256 base, Hash256 block){
        //Check if block is descendent of another base block
        return true;
    }

    public boolean verifyWithVoterSet() {
        return true;
    }

    public boolean verify() {

        return true;
    }
}
