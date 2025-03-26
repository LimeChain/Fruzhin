package com.limechain.consensus.beefy;

import com.limechain.consensus.beefy.dto.BeefyPayloadId;
import com.limechain.consensus.beefy.dto.BeefySession;
import com.limechain.consensus.beefy.dto.Commitment;
import com.limechain.consensus.beefy.dto.PayloadElement;
import com.limechain.consensus.beefy.dto.RoundAction;
import com.limechain.consensus.beefy.dto.VoteImportResult;
import com.limechain.consensus.beefy.dto.message.BeefyConsensusMessage;
import com.limechain.consensus.beefy.event.FinalizedBlockChangeEvent;
import com.limechain.consensus.beefy.event.FinalizedBlockChangeListener;
import com.limechain.exception.beefy.BeefyGenericException;
import com.limechain.exception.storage.BlockStorageGenericException;
import com.limechain.network.protocol.beefy.messages.justification.SignedCommitment;
import com.limechain.network.protocol.beefy.messages.vote.VoteMessage;
import com.limechain.network.protocol.warp.DigestHelper;
import com.limechain.network.protocol.warp.dto.BlockHeader;
import com.limechain.state.StateManager;
import com.limechain.storage.block.state.BlockState;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Log
@Component
@RequiredArgsConstructor
public class BeefyService implements FinalizedBlockChangeListener {

    private static final int MIN_BLOCK_DELTA = 1;

    private final StateManager stateManager;

    @Override
    public void finalizedBlockChanged(FinalizedBlockChangeEvent event) {
        stateManager.getBeefyState().setGrandpaFinalized(event.getGrandpaFinalized().getBlockNumber());
        processConsensusMessages(event.getBlockHeaders());
    }

    public void vote() {
        BeefyState beefyState = stateManager.getBeefyState();
        // Get the first session (round)
        BeefySession sessionStart = beefyState.getSessions().getFirst();

        // If no session is found, exit the method
        if (sessionStart == null) {
            log.warning("Vote BEEFY: No voting round started");
            return;
        }

        BigInteger sessionStartBlock = sessionStart.getMandatoryBlock();

        // Calculate the target vote block number
        BigInteger targetVoteBlockNumber;

        BigInteger beefyFinalized = beefyState.getBeefyFinalized();
        if (Objects.isNull(beefyFinalized)) {
            throw new BeefyGenericException("Beefy finalized is not initialized yet.");
        }

        BigInteger grandpaFinalized = beefyState.getGrandpaFinalized();

        // If the mandatory block (sessionStart) does not have a beefy justification yet, vote on it
        if (beefyFinalized.compareTo(sessionStartBlock) < 0) {
            log.info(String.format("Vote BEEFY: vote target - mandatory block: #%s%n", sessionStartBlock));
            targetVoteBlockNumber = sessionStartBlock;
        } else {
            if (Objects.isNull(grandpaFinalized)) {
                throw new BeefyGenericException("Grandpa finalized is not initialized yet.");
            }

            BigInteger diff = grandpaFinalized
                    .subtract(beefyFinalized)
                    .max(BigInteger.ZERO)
                    .add(BigInteger.ONE);
            int diffInt = diff.min(BigInteger.valueOf(Integer.MAX_VALUE)).intValue();
            int nextPowerOfTwo = (Integer.bitCount(diffInt) == 1) ? diffInt : Integer.highestOneBit(diffInt) << 1;
            int adjustedDiff = Math.max(MIN_BLOCK_DELTA, nextPowerOfTwo);

            targetVoteBlockNumber = beefyFinalized.add(BigInteger.valueOf(adjustedDiff));

            log.info(String.format("Vote BEEFY: vote target - diff: %d, next_power_of_two: %d, target block: #%s%n",
                    diffInt, nextPowerOfTwo, targetVoteBlockNumber));
        }

        // Don't vote for targets until they've been finalized (`target` can be > `bestGrandpa` when `minDelta` is big enough).
        // Also, ensure it's not voting on a block that has already been voted on.
        if (targetVoteBlockNumber.compareTo(grandpaFinalized) > 0
                || targetVoteBlockNumber.compareTo(beefyState.getLastVoted()) <= 0) {
            return; // No voting if target is beyond grandpa finalized or it's not a new block
        }

        // If it's a valid vote target, update the last voted block
        beefyState.setLastVoted(targetVoteBlockNumber);

        // TODO: Get Beefy Keys
        // TODO: Create Commitment and signature
        // TODO: Broadcast Vote Message
    }

    private Optional<SignedCommitment> handleVote(VoteMessage voteMessage) {
        BeefyState beefyState = stateManager.getBeefyState();
        BeefySession session = beefyState.getSessions().peekFirst();
        BigInteger blockNumber = voteMessage.getCommitment().getBlockNumber();

        if (session == null) {
            throw new BeefyGenericException("No beefy session exists.");
        }

        VoteImportResult result = session.addVote(voteMessage);

        switch (result) {
            case VoteImportResult.RoundConcluded voteImportResult -> {
                SignedCommitment signedCommitment = voteImportResult.signedCommitment();
                finalizeJustification(signedCommitment);
                return Optional.of(signedCommitment);
            }
            case VoteImportResult.Ok _ -> {
                if (!session.isMandatoryBlockFinalized() && session.getMandatoryBlock().equals(blockNumber)) {
                    //TODO: persist vote message
                }
            }
            case VoteImportResult.DoubleVoting _ -> {
                //TODO: report double voting
            }
            case VoteImportResult.Invalid _ -> log.info("handleVote: received an invalid/stale vote: " + voteMessage);
        }
        return Optional.empty();
    }

    /**
     * Examines BEEFY authority consensus messages and detects authority set changes or disabled authorities.
     * <p>
     * Upon encountering BEEFY_CHANGED_AUTHORITIES message, it finds keyPair, based on public keys,
     * and extracts the authority set. New BeefySession is created and added to the collection.
     * <p>
     * If a BEEFY_ON_DISABLED message is found, it updates the beefyState with the disabled authority information.
     */
    private void processConsensusMessages(List<BlockHeader> headers) {

        for (BlockHeader currentHeader : headers) {
            DigestHelper.getBeefyConsensusMessages(currentHeader.getDigest())
                    .forEach(cm -> stateManager.getBeefyState().handleBeefyConsensusMessage(
                            cm, currentHeader.getBlockNumber())
                    );
        }
    }

    private Commitment getCommitment(BigInteger blockNumber, BigInteger setId) {
        BlockState blockState = stateManager.getBlockState();
        BlockHeader blockHeader;
        try {
            blockHeader = blockState.getHeaderByNumber(blockNumber);
        } catch (BlockStorageGenericException e) {
            log.warning(String.format("getCommitment: Failed to retrieve block header for block number: %d. " +
                    "Exception: %s", blockNumber, e.getMessage()));
            return null;
        }
        Optional<byte[]> mmrHashOptional = extractMmrRootHash(blockHeader);
        if (mmrHashOptional.isEmpty()) {
            log.warning("extractMmrRootHash: Failed to retrieve mmr for the target block.");
            return null;
        }
        PayloadElement payloadElement = new PayloadElement(BeefyPayloadId.MMR, mmrHashOptional.get());

        return new Commitment(Collections.singletonList(payloadElement), blockNumber, setId);
    }

    private Optional<byte[]> extractMmrRootHash(BlockHeader blockHeader) {
        return DigestHelper.getBeefyConsensusMessages(blockHeader.getDigest())
                .stream().map(BeefyConsensusMessage::getMmrRootHash)
                .findFirst();
    }

    private void triageIncomingVote(VoteMessage voteMessage) {

        BigInteger blockNumber = voteMessage.getCommitment().getBlockNumber();
        RoundAction roundAction = determineRoundAction(blockNumber);

        switch (roundAction) {
            case RoundAction.PROCESS -> {
                log.fine(String.format("triageIncomingVotes: Process vote %s  for round: %d.", voteMessage, blockNumber));
                Optional<SignedCommitment> finalityProof = handleVote(voteMessage);
                if (finalityProof.isPresent()) {
                    //TODO: gossip vote message
                }
            }
            case RoundAction.ENQUEUE -> {
                log.fine(String.format("triageIncomingVotes: Unexpected vote: %s", voteMessage));
            }
            case RoundAction.DROP -> {
                log.fine(String.format("triageIncomingVotes: Drop vote  %s for round: %d.", voteMessage, blockNumber));
            }
            case RoundAction.INVALID -> {
                log.fine(String.format("triageIncomingVotes: Invalidate vote  %s for round: %d.", voteMessage, blockNumber));
            }
        }
    }

    private void triageIncomingJustification(SignedCommitment signedCommitment) {
        BigInteger blockNumber = signedCommitment.getCommitment().getBlockNumber();
        RoundAction roundAction = determineRoundAction(blockNumber);

        switch (roundAction) {
            case RoundAction.PROCESS -> {
                log.fine(String.format("triageIncomingJustification: Process justification for round: %d.", blockNumber));
                finalizeJustification(signedCommitment);
            }
            case RoundAction.ENQUEUE -> {
                log.fine(String.format("triageIncomingJustification: Enqueue justification for round: %d.", blockNumber));
                stateManager.getBeefyState().getPendingJustifications().put(blockNumber, signedCommitment);
            }
            case RoundAction.DROP -> {
                log.fine(String.format("triageIncomingJustification: Drop justification for round: %d.", blockNumber));
            }
            case RoundAction.INVALID -> {
                log.fine(String.format("triageIncomingJustification: Invalidate justification for round: %d.", blockNumber));
            }
        }
    }

    private RoundAction determineRoundAction(BigInteger roundNumber) {
        Pair<BigInteger, BigInteger> roundsInterval = null;
        try {
            roundsInterval = findAcceptedRoundsInterval();
        } catch (BeefyGenericException e) {
            log.warning(String.format("determineRoundAction: Error while finding accepted rounds interval %s", e));
            return RoundAction.INVALID;
        }
        BigInteger startRoundNumber = roundsInterval.getLeft();
        BigInteger endRoundNumber = roundsInterval.getRight();

        if (roundNumber.compareTo(startRoundNumber) >= 0 && roundNumber.compareTo(endRoundNumber) <= 0) {
            return RoundAction.PROCESS;
        } else if (roundNumber.compareTo(endRoundNumber) > 0) {
            return RoundAction.ENQUEUE;
        } else {
            return RoundAction.DROP;
        }
    }

    private Pair<BigInteger, BigInteger> findAcceptedRoundsInterval() {
        BeefyState beefyState = stateManager.getBeefyState();

        BeefySession currentSession = beefyState.getSessions().peekFirst();
        if (currentSession == null) {
            throw new BeefyGenericException("No beefy session exists.");
        }

        BigInteger beefyFinalized = beefyState.getBeefyFinalized();
        if (beefyFinalized == null) {
            throw new BeefyGenericException("Beefy finalized is not initialized yet.");
        }

        if (currentSession.isMandatoryBlockFinalized()) {
            BigInteger lowerBlock = beefyFinalized.max(currentSession.getMandatoryBlock());
            return Pair.of(lowerBlock, beefyState.getGrandpaFinalized());
        } else {
            return Pair.of(currentSession.getMandatoryBlock(), currentSession.getMandatoryBlock());
        }
    }

    private void finalizeJustification(SignedCommitment signedCommitment) {
        BeefyState beefyState = stateManager.getBeefyState();
        BigInteger blockNumber = signedCommitment.getCommitment().getBlockNumber();
        if (blockNumber.compareTo(beefyState.getBeefyFinalized()) <= 0) {
            log.fine(String.format("finalizeJustification: Round: %d has been already finalized.", blockNumber));
            return;
        }

        try {
            finalizeBeefyRound(blockNumber);
        } catch (BeefyGenericException e) {
            log.warning(String.format("finalizeJustification: Error while finalizing beefy round: %s", e));
            return;
        }
        beefyState.setBeefyFinalized(blockNumber);

        //TODO: Persist beefy state
    }

    private void finalizeBeefyRound(BigInteger blockNumber) {
        BeefyState beefyState = stateManager.getBeefyState();
        BeefySession currentSession = beefyState.getSessions().peekFirst();
        if (currentSession == null) {
            throw new BeefyGenericException("No beefy session exists.");
        }

        currentSession.update(blockNumber);
        removeFinishedSessions();
    }

    private void removeFinishedSessions() {
        BeefyState beefyState = stateManager.getBeefyState();
        if (beefyState.getSessions().size() > 1) {
            beefyState.getSessions().removeIf(BeefySession::isMandatoryBlockFinalized);
        }
    }
}
