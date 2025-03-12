package com.limechain.network.protocol.warp.dto;

import com.limechain.consensus.grandpa.dto.SignedVote;
import com.limechain.network.protocol.grandpa.messages.catchup.res.CatchUpResMessage;
import com.limechain.network.protocol.grandpa.messages.commit.CommitMessage;
import io.emeraldpay.polkaj.types.Hash256;
import lombok.Getter;
import lombok.Setter;

import java.math.BigInteger;
import java.util.Arrays;

@Setter
@Getter
public class Justification {

    private BigInteger roundNumber;
    private Hash256 targetHash;
    private BigInteger targetBlock;
    private SignedVote[] signedVotes; // either preCommis or preVotes
    // TODO: Review handling (unclear purpose of ancestryVotes)
    private BlockHeader[] ancestryVotes;

    public static Justification fromCommitMessage(CommitMessage commitMessage) {
        Justification justification = new Justification();
        justification.setRoundNumber(commitMessage.getRoundNumber());
        justification.setTargetHash(commitMessage.getVote().getBlockHash());
        justification.setTargetBlock(commitMessage.getVote().getBlockNumber());
        justification.setSignedVotes(commitMessage.getPreCommits());

        return justification;
    }

    public static Justification fromCatchUpResPreVotes(CatchUpResMessage message) {
        return fromCatchUpResMessage(message, message.getPreVotes());
    }

    public static Justification fromCatchUpResPreCommits(CatchUpResMessage message) {
        return fromCatchUpResMessage(message, message.getPreCommits());
    }

    private static Justification fromCatchUpResMessage(CatchUpResMessage message, SignedVote[] signedVotes) {
        Justification justification = new Justification();
        justification.setRoundNumber(message.getRoundNumber());
        justification.setTargetHash(message.getBlockHash());
        justification.setTargetBlock(message.getBlockNumber());
        justification.setSignedVotes(signedVotes);

        return justification;
    }

    @Override
    public String toString() {
        return "Justification{" +
                "roundNumber=" + roundNumber +
                ", targetHash=" + targetHash +
                ", targetBlock=" + targetBlock +
                ", signedVotes=" + Arrays.toString(signedVotes) +
                ", ancestryVotes=" + Arrays.toString(ancestryVotes) +
                '}';
    }
}
