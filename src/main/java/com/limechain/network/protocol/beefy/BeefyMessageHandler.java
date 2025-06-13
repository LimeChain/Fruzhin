package com.limechain.network.protocol.beefy;

import com.limechain.consensus.beefy.BeefyService;
import com.limechain.consensus.beefy.BeefyState;
import com.limechain.consensus.beefy.dto.BeefyAuthoritySet;
import com.limechain.consensus.beefy.dto.BeefySession;
import com.limechain.consensus.beefy.dto.Commitment;
import com.limechain.consensus.beefy.scale.CommitmentScaleWriter;
import com.limechain.exception.beefy.BeefyGenericException;
import com.limechain.network.protocol.beefy.messages.justification.SignedCommitment;
import com.limechain.network.protocol.beefy.messages.vote.BeefyVoteMessage;
import com.limechain.runtime.hostapi.dto.Key;
import com.limechain.runtime.hostapi.dto.VerifySignature;
import com.limechain.state.StateManager;
import com.limechain.utils.EcdsaUtils;
import com.limechain.utils.HashUtils;
import com.limechain.utils.scale.ScaleUtils;
import io.emeraldpay.polkaj.types.Hash264;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.javatuples.Pair;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

@Log
@Component
@RequiredArgsConstructor
public class BeefyMessageHandler {

    private final Object lock = new Object();

    private final BeefyService beefyService;
    private final StateManager stateManager;

    public void handleVoteMessage(BeefyVoteMessage voteMessage) {

        BeefyState beefyState = stateManager.getBeefyState();
        Commitment commitment = voteMessage.getCommitment();

        if (beefyState.getSessions().isEmpty() || !beefyService.isBeefyMessageAcceptable(commitment)) {
            return;
        }

        synchronized (lock) {
            BeefySession currentSession = beefyState.getSessions().peekFirst();
            Hash264 authorityIdHash = new Hash264(voteMessage.getAuthorityId());
            BigInteger blockNumber = voteMessage.getCommitment().getBlockNumber();
            Pair<Hash264, BigInteger> voteKey = Pair.with(authorityIdHash, blockNumber);

            if (currentSession.getPreviousVotes().containsKey(voteKey)) {
                log.fine(String.format(
                        "handleBeefyVoteMessage: Skipping already known vote message from authority: %s for block: %s.",
                        authorityIdHash, blockNumber
                ));
            }

            if (!isVoteMessageValid(voteMessage)) {
                log.warning(String.format(
                        "handleBeefyVoteMessage: Invalid vote message for round %s, set %s",
                        commitment.getBlockNumber(), commitment.getAuthoritySetId()
                ));
                return;
            }
            beefyService.triageIncomingVote(voteMessage);
        }
    }

    public void handleSignedCommitment(SignedCommitment signedCommitment) {

        BeefyState beefyState = stateManager.getBeefyState();
        if (beefyState.getSessions().isEmpty() ||
                !beefyService.isBeefyMessageAcceptable(signedCommitment.getCommitment())) {
            return;
        }

        if (!isJustificationValid(signedCommitment)) {
            log.warning(String.format(
                    "handleSignedCommitment: Invalid justification message for round %s, set %s",
                    signedCommitment.getCommitment().getBlockNumber(), signedCommitment.getCommitment().getAuthoritySetId()
            ));
            return;
        }

        beefyService.triageIncomingJustification(signedCommitment);
    }

    private boolean isJustificationValid(SignedCommitment signedCommitment) {
        BeefyState beefyState = stateManager.getBeefyState();
        Commitment commitment = signedCommitment.getCommitment();
        List<Optional<byte[]>> signatures = signedCommitment.getSignatures();
        BeefySession beefySession = beefyState.getSessions().peekFirst();

        if (beefySession == null) {
            throw new BeefyGenericException("No active Beefy session found.");
        }

        BeefyAuthoritySet authoritySet = beefySession.getAuthoritySet();

        if (commitment.getBlockNumber().compareTo(beefySession.getMandatoryBlock()) < 0 ||
                signatures.size() != authoritySet.getPublicKeys().size() ||
                !authoritySet.getSetId().equals(commitment.getAuthoritySetId())) {
            return false;
        }

        BigInteger validSignaturesCount = countValidSignatures(commitment, authoritySet, signatures);
        return validSignaturesCount.compareTo(beefySession.getThreshold()) >= 0;
    }


    private BigInteger countValidSignatures(Commitment commitment,
                                            BeefyAuthoritySet authoritySet,
                                            List<Optional<byte[]>> signatures) {

        byte[] encodedCommitment = HashUtils.hashWithKeccak256(
                ScaleUtils.Encode.encode(CommitmentScaleWriter.getInstance(), commitment)
        );

        BigInteger validSignaturesCount = BigInteger.ZERO;
        List<byte[]> publicKeys = authoritySet.getPublicKeys();

        for (int i = 0; i < publicKeys.size(); i++) {
            byte[] authorityId = publicKeys.get(i);
            Optional<byte[]> maybeSignature = signatures.get(i);

            if (maybeSignature.isEmpty()) {
                continue;
            }

            VerifySignature verifySignature = new VerifySignature(
                    maybeSignature.get(), encodedCommitment, authorityId, Key.ECDSA
            );

            try {
                if (EcdsaUtils.verifySignature(verifySignature)) {
                    validSignaturesCount = validSignaturesCount.add(BigInteger.ONE);
                }
            } catch (IllegalArgumentException e) {
                log.fine("countValidSignatures: Failed to verify signature: " + e.getMessage());
            }
        }

        return validSignaturesCount;
    }

    private boolean isVoteMessageValid(BeefyVoteMessage voteMessage) {

        byte[] encodedCommitment = HashUtils.hashWithKeccak256(ScaleUtils.Encode.encode(CommitmentScaleWriter.getInstance(),
                voteMessage.getCommitment()));

        VerifySignature verifySignature = new VerifySignature(
                voteMessage.getSignature(),
                encodedCommitment,
                voteMessage.getAuthorityId(),
                Key.ECDSA);

        return EcdsaUtils.verifySignature(verifySignature);
    }
}
