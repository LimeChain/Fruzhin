package com.limechain.consensus.beefy.dto;

import com.limechain.network.protocol.beefy.messages.justification.SignedCommitment;

public sealed interface VoteImportResult permits VoteImportResult.Ok, VoteImportResult.RoundConcluded,
        VoteImportResult.DoubleVoting, VoteImportResult.Invalid{

    record Ok() implements VoteImportResult {}

    record RoundConcluded(SignedCommitment signedCommitment) implements VoteImportResult {}

    record DoubleVoting(DoubleVotingProof doubleVotingProof) implements VoteImportResult {}

    record Invalid() implements VoteImportResult {}
}