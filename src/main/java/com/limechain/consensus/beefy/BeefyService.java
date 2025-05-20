package com.limechain.consensus.beefy;

import com.limechain.consensus.beefy.dto.BeefyAuthoritySet;
import com.limechain.consensus.beefy.dto.BeefyPayloadId;
import com.limechain.consensus.beefy.dto.BeefySession;
import com.limechain.consensus.beefy.dto.Commitment;
import com.limechain.consensus.beefy.dto.DoubleVotingProof;
import com.limechain.consensus.beefy.dto.PayloadElement;
import com.limechain.consensus.beefy.dto.RoundAction;
import com.limechain.consensus.beefy.dto.VoteImportResult;
import com.limechain.consensus.beefy.dto.message.BeefyConsensusMessage;
import com.limechain.consensus.beefy.dto.message.BeefyConsensusMessageFormat;
import com.limechain.consensus.beefy.event.FinalizedBlockChangeEvent;
import com.limechain.consensus.beefy.event.FinalizedBlockChangeListener;
import com.limechain.consensus.beefy.scale.CommitmentScaleWriter;
import com.limechain.exception.beefy.BeefyGenericException;
import com.limechain.exception.storage.BlockStorageGenericException;
import com.limechain.network.PeerMessageCoordinator;
import com.limechain.network.protocol.beefy.messages.justification.SignedCommitment;
import com.limechain.network.protocol.beefy.messages.vote.BeefyVoteMessage;
import com.limechain.network.protocol.warp.DigestHelper;
import com.limechain.network.protocol.warp.dto.BlockHeader;
import com.limechain.runtime.Runtime;
import com.limechain.state.StateManager;
import com.limechain.storage.block.state.BlockState;
import com.limechain.utils.EcdsaUtils;
import com.limechain.utils.HashUtils;
import com.limechain.utils.scale.ScaleUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.javatuples.Pair;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Log
@Component
@RequiredArgsConstructor
public class BeefyService implements FinalizedBlockChangeListener {

    private static final int MIN_BLOCK_DELTA = 8;

    private final StateManager stateManager;
    private final PeerMessageCoordinator peerMessageCoordinator;

    private Pair<BigInteger, BigInteger> cachedAcceptedInterval;

    @Override
    public void finalizedBlockChanged(FinalizedBlockChangeEvent event) {

        BeefyState beefyState = stateManager.getBeefyState();
        BlockState blockState = stateManager.getBlockState();

        beefyState.setGrandpaFinalized(event.getGrandpaFinalized().getBlockNumber());

        if (event.getGrandpaFinalized().getBlockNumber().equals(beefyState.getBeefyGenesis())) {
            beefyState.handleChangedBeefyAuthorities(
                    beefyState.getAuthoritySet().getPublicKeys(),
                    beefyState.getAuthoritySet().getSetId(),
                    beefyState.getGrandpaFinalized()
            );
        } else {
            processConsensusMessages(event.getBlockHeaders());
        }

        Runtime runtime = blockState.getRuntime(event.getGrandpaFinalized().getHash());
        BigInteger newGenesis = runtime.getBeefyGenesis().orElse(null);

        if (!Objects.equals(newGenesis, beefyState.getBeefyGenesis())) {

            beefyState.setLastVote(null);
            if (newGenesis != null) {

                if (beefyState.getBeefyFinalized().compareTo(newGenesis) < 0) {
                    beefyState.setBeefyFinalized(BigInteger.ZERO);
                }

                beefyState.getSessions()
                        .removeIf(session -> session.getMandatoryBlock().compareTo(newGenesis) < 0);
                beefyState.getPendingJustifications().entrySet()
                        .removeIf(entry ->
                                entry.getValue().getCommitment().getBlockNumber().compareTo(newGenesis) < 0);
            } else {
                beefyState.getSessions().clear();
                beefyState.getPendingJustifications().clear();
            }

            beefyState.setBeefyGenesis(newGenesis);
        }

        beefyState.persistState();

        // update beefy message cached interval
        cachedAcceptedInterval = findAcceptedInterval();
    }

    public void vote() {

        BeefyState beefyState = stateManager.getBeefyState();
        // Get the first session (round)
        if (beefyState.getSessions().isEmpty()) {
            log.warning("vote: No session exists.");
            return;
        }

        BeefySession sessionStart = beefyState.getSessions().peekFirst();
        BigInteger sessionStartBlock = sessionStart.getMandatoryBlock();
        BigInteger grandpaFinalized = beefyState.getGrandpaFinalized();
        BigInteger beefyFinalized = beefyState.getBeefyFinalized();

        BigInteger targetVoteBlockNumber = beefyState.getTargetVoteBlockNumber();

        BigInteger lastVoted = beefyState.getLastVoted();
        // Don't vote for targets until they've been finalized (`target` can be > `grandpaFinalized`
        // when `MIN_BLOCK_DELTA` is big enough).
        // Also, ensure it's not voting on a block that has already been voted on.
        if (lastVoted != null && shouldSkipVote(targetVoteBlockNumber, grandpaFinalized, lastVoted)) {
            beefyState.setTargetVoteBlockNumber(
                    calculateTargetVoteBlockNumber(
                            sessionStartBlock,
                            beefyFinalized,
                            grandpaFinalized
                    ));
            return; // No voting if target is beyond grandpa finalized, or it's not a new block
        }

        Pair<byte[], byte[]> keyPair = sessionStart.getBeefyKeyPair();
        if (keyPair == null) return;

        BeefyVoteMessage voteMessage = createVoteMessage(
                sessionStart.getAuthoritySet(),
                keyPair,
                targetVoteBlockNumber
        );

        handleVote(voteMessage).ifPresentOrElse(
                peerMessageCoordinator::sendSignedCommitmentToPeers,
                () -> peerMessageCoordinator.sendBeefyVoteMessageToPeers(voteMessage)
        );

        // If it's a valid vote target, update the last voted block
        beefyState.setLastVoted(targetVoteBlockNumber);
        beefyState.persistState();
    }

    private BeefyVoteMessage createVoteMessage(BeefyAuthoritySet authoritySet,
                                               Pair<byte[], byte[]> keyPair,
                                               BigInteger targetVoteBlockNumber) {

        byte[] publicKey = keyPair.getValue0();
        byte[] privateKey = keyPair.getValue1();

        Commitment commitment = getCommitment(targetVoteBlockNumber, authoritySet.getSetId());
        byte[] encodedCommitment = ScaleUtils.Encode.encode(CommitmentScaleWriter.getInstance(), commitment);
        byte[] hashedCommitment = HashUtils.hashWithKeccak256(encodedCommitment);

        byte[] signature = EcdsaUtils.signMessage(privateKey, hashedCommitment);

        if (signature == null) {
            throw new BeefyGenericException("createVoteMessage: Failed to generate signature for the commitment " +
                    "with block number: " + targetVoteBlockNumber);
        }

        return new BeefyVoteMessage(commitment, publicKey, signature);
    }

    private BigInteger calculateTargetVoteBlockNumber(BigInteger sessionStartBlock,
                                                      BigInteger beefyFinalized,
                                                      BigInteger grandpaFinalized) {
        BigInteger targetVoteBlockNumber;

        // If the mandatory block (sessionStart) does not have a beefy justification yet, vote on it
        if (beefyFinalized.compareTo(sessionStartBlock) < 0) {
            log.fine(String.format("Vote BEEFY: vote target - mandatory block: #%s", sessionStartBlock));
            targetVoteBlockNumber = sessionStartBlock;
        } else {

            BigInteger diff = grandpaFinalized
                    .subtract(beefyFinalized)
                    .max(BigInteger.ZERO)
                    .add(BigInteger.ONE);

            int diffInt = diff.min(BigInteger.valueOf(Integer.MAX_VALUE)).intValueExact();
            int nextPowerOfTwo = (Integer.bitCount(diffInt) == 1) ? diffInt : Integer.highestOneBit(diffInt) << 1;
            int adjustedDiff = Math.max(MIN_BLOCK_DELTA, nextPowerOfTwo);

            targetVoteBlockNumber = beefyFinalized.add(BigInteger.valueOf(adjustedDiff));

            log.fine(String.format("Vote BEEFY: vote target - diff: %d, next_power_of_two: %d, target block: #%s",
                    diffInt, nextPowerOfTwo, targetVoteBlockNumber));
        }

        return targetVoteBlockNumber;
    }

    private boolean shouldSkipVote(BigInteger targetVoteBlockNumber,
                                   BigInteger grandpaFinalized,
                                   BigInteger lastVoted) {

        return targetVoteBlockNumber.compareTo(grandpaFinalized) > 0
                || targetVoteBlockNumber.compareTo(lastVoted) <= 0;
    }

    private Optional<SignedCommitment> handleVote(BeefyVoteMessage voteMessage) {

        BeefyState beefyState = stateManager.getBeefyState();
        BeefySession session = beefyState.getSessions().peekFirst();
        BigInteger blockNumber = voteMessage.getCommitment().getBlockNumber();

        if (session == null) {
            throw new BeefyGenericException("handleVote: No beefy session exists.");
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
                    stateManager.getBeefyState().persistState();
                }
            }
            case VoteImportResult.DoubleVoting voteImportResult ->
                    reportDoubleVoting(voteImportResult.doubleVotingProof());
            case VoteImportResult.Invalid _ -> log.info("handleVote: received an invalid/stale vote: " + voteMessage);
        }
        return Optional.empty();
    }

    private void reportDoubleVoting(DoubleVotingProof doubleVotingProof) {

        BlockState blockState = stateManager.getBlockState();
        Runtime runtime = blockState.getRuntime(blockState.getHighestFinalizedHash());
        runtime.generateBeefyKeyOwnershipProof(doubleVotingProof.getFirst().getCommitment().getAuthoritySetId(),
                        doubleVotingProof.getFirst().getAuthorityId())
                .ifPresentOrElse(key ->
                                runtime.submitReportBeefyDoubleVotingUnsignedExtrinsic(
                                        doubleVotingProof, key.getProof()
                                ),
                        () -> log.warning(String.format(
                                "reportDoubleVoting: Failed to report Beefy double voting for block number: %s.",
                                doubleVotingProof.getFirst().getCommitment().getBlockNumber()
                        ))
                );
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
        return DigestHelper.getBeefyConsensusMessages(blockHeader.getDigest()).stream()
                .filter(cm -> BeefyConsensusMessageFormat.BEEFY_MMR_ROOT.equals(cm.getFormat()))
                .map(BeefyConsensusMessage::getMmrRootHash)
                .findFirst();
    }

    public void triageIncomingVote(BeefyVoteMessage voteMessage) {

        BigInteger blockNumber = voteMessage.getCommitment().getBlockNumber();
        RoundAction roundAction = determineRoundAction(blockNumber);

        switch (roundAction) {
            case RoundAction.PROCESS -> {
                log.fine(String.format("triageIncomingVotes: Process vote %s for round: %d.", voteMessage, blockNumber));

                handleVote(voteMessage).ifPresentOrElse(
                        peerMessageCoordinator::sendSignedCommitmentToPeers,
                        () -> peerMessageCoordinator.sendBeefyVoteMessageToPeers(voteMessage)
                );
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

    public void triageIncomingJustification(SignedCommitment signedCommitment) {

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
        BeefyState beefyState = stateManager.getBeefyState();

        if (beefyState.getSessions().isEmpty()) {
            log.fine("determineRoundAction: No beefy session exists.");
            return RoundAction.INVALID;
        }
        Pair<BigInteger, BigInteger> roundsInterval = findAcceptedInterval();
        BigInteger startRoundNumber = roundsInterval.getValue0();
        BigInteger endRoundNumber = roundsInterval.getValue1();

        if (roundNumber.compareTo(startRoundNumber) >= 0 && roundNumber.compareTo(endRoundNumber) <= 0) {
            return RoundAction.PROCESS;
        } else if (roundNumber.compareTo(endRoundNumber) > 0) {
            return RoundAction.ENQUEUE;
        } else {
            return RoundAction.DROP;
        }
    }

    private Pair<BigInteger, BigInteger> findAcceptedInterval() {

        BeefyState beefyState = stateManager.getBeefyState();
        if (beefyState.getSessions().isEmpty()) {
            return new Pair<>(BigInteger.ZERO, BigInteger.ZERO);
        }

        BeefySession currentSession = beefyState.getSessions().peekFirst();
        BigInteger mandatoryBlock = currentSession.getMandatoryBlock();

        if (currentSession.isMandatoryBlockFinalized()) {
            BigInteger lowerBlock = beefyState.getBeefyFinalized().max(currentSession.getMandatoryBlock());
            return Pair.with(lowerBlock, beefyState.getGrandpaFinalized());
        } else {
            return Pair.with(mandatoryBlock, mandatoryBlock);
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
            log.warning(String.format("finalizeJustification: Error while finalizing beefy round: %s", e.getMessage()));
            return;
        }

        log.info(String.format("finalizeJustification: Round: %d has been finalized.", blockNumber));
        beefyState.setBeefyFinalized(blockNumber);
        beefyState.persistState();

        // update beefy message cached interval
        cachedAcceptedInterval = findAcceptedInterval();
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

    private void applyPendingJustifications() {

        BeefyState beefyState = stateManager.getBeefyState();

        if (beefyState.getPendingJustifications().isEmpty()) return;

        if (beefyState.getSessions().isEmpty()) {
            log.warning("applyPendingJustifications: No session exists.");
            return;
        }

        Pair<BigInteger, BigInteger> roundsInterval = findAcceptedInterval();
        BigInteger start = roundsInterval.getValue0();
        BigInteger end = roundsInterval.getValue1();

        LinkedHashMap<BigInteger, SignedCommitment> stillPending = new LinkedHashMap<>();
        LinkedHashMap<BigInteger, SignedCommitment> justificationsToProcess = new LinkedHashMap<>();

        beefyState.getPendingJustifications().forEach((key, value) -> {
            if (key.compareTo(start) >= 0 && key.compareTo(end) <= 0) {
                justificationsToProcess.put(key, value);
            } else if (key.compareTo(end) > 0) {
                stillPending.put(key, value);
            }
        });

        // Update pendingJustification field in the state
        beefyState.setPendingJustifications(stillPending);

        // Process justification that are in the accepted interval
        justificationsToProcess.values().forEach(this::finalizeJustification);
    }

    private void requestMandatoryJustification() {

        BeefyState beefyState = stateManager.getBeefyState();

        if (beefyState.getSessions().isEmpty()) {
            log.warning("requestMandatoryJustification: No session exists.");
            return;
        }

        BeefySession session = beefyState.getSessions().peekFirst();
        if (!session.isMandatoryBlockFinalized()) {
            //TODO: check if that isn't spamming
            log.info(String.format("requestMandatoryJustification: Session %s is not mandatory.", session.getMandatoryBlock()));
            beefyState.requestJustification(session.getMandatoryBlock());
        }
    }

    public boolean isBeefyMessageAcceptable(Commitment commitment) {
        BeefyState beefyState = stateManager.getBeefyState();
        BigInteger blockNumber = commitment.getBlockNumber();
        BeefySession currentSession = beefyState.getSessions().peekFirst();

        BigInteger mandatoryBlock = currentSession.getMandatoryBlock();
        if (blockNumber.compareTo(mandatoryBlock) < 0) {
            log.fine(String.format(
                    "isBeefyMessageAcceptable: " +
                            "Rejected beefy message — block %d is earlier than current session's mandatory block %d.",
                    blockNumber, mandatoryBlock
            ));

            return false;
        }

        BigInteger setId = currentSession.getAuthoritySet().getSetId();
        BigInteger commitmentSetId = commitment.getAuthoritySetId();
        if (!setId.equals(commitmentSetId)) {
            log.fine(String.format(
                    "isBeefyMessageAcceptable: Rejected beefy message — authority set ID mismatch. Expected: %d, got: %d.",
                    setId, commitmentSetId
            ));

            return false;
        }

        if (cachedAcceptedInterval == null) {
            cachedAcceptedInterval = findAcceptedInterval();
        }

        BigInteger start = cachedAcceptedInterval.getValue0();
        BigInteger end = cachedAcceptedInterval.getValue1();

        if (blockNumber.compareTo(start) < 0 || blockNumber.compareTo(end) > 0) {
            log.fine(String.format(
                    "isBeefyMessageAcceptable: Rejected beefy message — block %d outside accepted round range [%d, %d].",
                    blockNumber, start, end
            ));

            return false;
        }

        log.fine(String.format(
                "isBeefyMessageAcceptable: Accepted beefy message — block %d is within round range [%d, %d] and set ID %d.",
                blockNumber, start, end, setId
        ));

        return true;
    }

    public void start() {

        log.info("start: Started Beefy Service main loop");
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleWithFixedDelay(() -> {
            try {
                if (shouldRun()) {
                    applyPendingJustifications();
                    vote();
                    requestMandatoryJustification();
                }
            } catch (Exception e) {
                log.warning("Exception in Beefy main loop, restarting in 1 second " + e.getMessage());
                //TODO: handle restarting of main loop
            }
        }, 0, 1, TimeUnit.MILLISECONDS);
    }

    private boolean shouldRun() {

        BeefyState beefyState = stateManager.getBeefyState();
        BlockState blockState = stateManager.getBlockState();

        BigInteger beefyGenesis = beefyState.getBeefyGenesis();
        BlockHeader lastFinalized = blockState.getHighestFinalizedHeader();

        if (beefyGenesis == null) {
            return false;
        }

        if (beefyState.getSessions().isEmpty()) {
            return false;
        }

        return lastFinalized.getBlockNumber().compareTo(beefyGenesis) >= 0;
    }
}
