package com.limechain.network.protocol.warp.dto;

import com.limechain.chain.lightsyncstate.Authority;
import io.emeraldpay.polkaj.types.Hash256;
import lombok.Setter;
import lombok.extern.java.Log;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
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

    public boolean verify(Authority[] authorities, BigInteger authoritiesSetId, byte[] randomness) {
        // TODO: implement https://github.com/smol-dot/smoldot/blob/165412f0292009aedd208615a37cf2859fd45936/lib/src/finality/justification/verify.rs#L50
        if (precommits.length < authorities.length * 2 / 3 + 1) {
            log.log(Level.WARNING, "Not enough signatures");
            return false;
        }

        Set<Hash256> seen_pub_keys = new HashSet<>();

        for (Precommit precommit : precommits) {
            if (!Arrays.stream(authorities).toList().contains(precommit.getAuthorityPublicKey())) {
                log.log(Level.WARNING, "Invalid Authority for precommit");
                return false;
            }

            if (seen_pub_keys.contains(precommit.getAuthorityPublicKey())) {
                log.log(Level.WARNING, "Duplicated signature");
                return false;
            }
            seen_pub_keys.add(precommit.getAuthorityPublicKey());

            // TODO (from smoldot): must check signed block ancestry using `votes_ancestries`

            List<Byte> message = new ArrayList<>();
            message.add(Byte.decode("1u8"));
            byte[] targetHash = precommit.getTargetHash().getBytes();
            for (byte targetHashByte : targetHash) {
                message.add(Byte.valueOf(targetHashByte));
            }

            byte[] targetNumberBytes = precommit.getTargetNumber().toByteArray();
            int blockNumberBytes = 4;
            int targetNumberSize = Math.min(targetNumberBytes.length, blockNumberBytes);

            for (int i = 0; i < targetNumberSize; i++) {
                message.add(Byte.valueOf(targetNumberBytes[i]));
            }

            for (int i = 0; i < blockNumberBytes - targetNumberSize; i++) {
                message.add(Byte.valueOf((byte) 0));
            }
            
        }

        return true;
    }
}
