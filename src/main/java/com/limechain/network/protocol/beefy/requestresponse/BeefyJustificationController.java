package com.limechain.network.protocol.beefy.requestresponse;

import com.limechain.exception.NotImplementedException;
import com.limechain.network.protocol.beefy.messages.justification.SignedCommitment;

import java.math.BigInteger;
import java.util.concurrent.CompletableFuture;

public interface BeefyJustificationController {

    default CompletableFuture<SignedCommitment> send(BigInteger req) {
        throw new NotImplementedException("Method not implemented!");
    }

    default CompletableFuture<SignedCommitment> sendJustificationRequest(BigInteger from) {
        return send(from);
    }
}
