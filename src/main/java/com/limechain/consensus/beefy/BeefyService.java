package com.limechain.consensus.beefy;

import com.limechain.consensus.beefy.dto.BeefyAuthoritySet;
import com.limechain.consensus.beefy.dto.BeefyPayloadId;
import com.limechain.consensus.beefy.dto.BeefyRound;
import com.limechain.consensus.beefy.dto.BeefySession;
import com.limechain.consensus.beefy.dto.Commitment;
import com.limechain.consensus.beefy.dto.PayloadElement;
import com.limechain.consensus.beefy.dto.message.BeefyConsensusMessage;
import com.limechain.exception.beefy.BeefyGenericException;
import com.limechain.exception.storage.BlockStorageGenericException;
import com.limechain.network.protocol.warp.DigestHelper;
import com.limechain.network.protocol.warp.dto.BlockHeader;
import com.limechain.state.StateManager;
import com.limechain.storage.block.state.BlockState;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Log
@Component
@RequiredArgsConstructor
public class BeefyService {

    private static final BigInteger THRESHOLD_DENOMINATOR = BigInteger.valueOf(3);
    private static final int MIN_BLOCK_DELTA = 1;

    private final StateManager stateManager;
    private final BeefyState beefyState;

    /**
     * The threshold is determined as the numOfValidators - (numOfValidators - 1) / 3
     *
     * @return minimum required validators for finality.
     */
    private BigInteger getThreshold() {
        BeefyAuthoritySet authoritySet = stateManager.getBeefyState().getAuthoritySet();

        if (Objects.isNull(authoritySet)) {
            log.warning("getThreshold: No authoritySet in BeefyState.");
            return BigInteger.ZERO;
        }

        var validatorSize = authoritySet.getPublicKeys().size();

        if (validatorSize == 0) {
            log.warning("getThreshold: Validator set is empty.");
            return BigInteger.ZERO;
        }

        var numOfValidators = BigInteger.valueOf(validatorSize);
        var faulty = (numOfValidators.subtract(BigInteger.ONE)).divide(THRESHOLD_DENOMINATOR);

        return numOfValidators.subtract(faulty);
    }

    public void vote() {
        BeefyState beefyState = stateManager.getBeefyState();
        // Get the first session (round)
        BeefySession sessionStart = beefyState.getSessions().getFirst();

        // If no session is found, exit the method
        if (sessionStart == null) {
            log.info("Vote BEEFY: No voting round started");
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


//    private void processJustification(SignedCommitment justification) {
//
//        //TODO: check for justification in db
//
//        BigInteger beefyFinalized = beefyState.getBeefyFinalized();
//        if (Objects.isNull(beefyFinalized)) {
//            throw new BeefyGenericException("Beefy finalized is not initialized yet.");
//        }
//
//
//        BigInteger blockNumber = justification.getCommitment().getBlockNumber();
//        Map<BigInteger, BeefySession> found = Collections.emptyMap();
//
//        BigInteger nextDigest = beefyState.getNextDigest();
//        if (Objects.isNull(nextDigest)) {
//            beefyState.initializeNextDigest();
//        }
//
//        if (blockNumber.compareTo(beefyFinalized) <= 0) {
//            found = retrieveMandatoryBlockAndCreateBeefySession(blockNumber, blockNumber);
//            if (found.isEmpty()) {
//                return;
//            }
//        } else if (blockNumber.compareTo(beefyState.getNextDigest()) >= 0) {
//            found = retrieveMandatoryBlockAndCreateBeefySession(blockNumber, beefyState.getNextDigest());
//        }
//
//        Map.Entry<BigInteger, BeefySession> currentSession = determineCurrentSessionOnJustification(blockNumber, found);
//        if (Objects.isNull(currentSession)) {
//            return;
//        }
//
//        BigInteger mandatoryBlock = found.isEmpty() ?
//                currentSession.getKey() : found.keySet().iterator().next();
//
//        BeefySession beefySession = found.isEmpty() ?
//                currentSession.getValue() : found.values().iterator().next();
//
//        //TODO: Validate justification
//
//        //TODO: Store justification in db if successfully verified
//
//        cleanUpSessionsAndRoundsOnJustification(currentSession, blockNumber);
//
//        LinkedHashMap<BigInteger, BeefySession> beefySessions = beefyState.getSessions();
//        if (!found.isEmpty()) {
//            // Store the newly found block and corresponding session, where authority change appeared
//            beefySessions.put(mandatoryBlock, beefySession);
//        }
//
//        //TODO: Update state
//    }
//
//    private Map.Entry<BigInteger, BeefySession> determineCurrentSessionOnJustification(BigInteger blockNumber,
//                                                                                       Map<BigInteger, BeefySession> found) {
//
//        LinkedHashMap<BigInteger, BeefySession> beefySessions = beefyState.getSessions();
//        // Initialize the current session with the last possible session that we have
//        Map.Entry<BigInteger, BeefySession> currentSession = beefySessions.lastEntry();
//
//        // If no new mandatory block and corresponding session are being found for the current block
//        if (found.isEmpty()) {
//            if (blockNumber.compareTo(beefyState.getBeefyFinalized()) <= 0) {
//                log.warning(String.format("Block: %d has already been finalized.", blockNumber));
//                return null;
//            }
//            // The mandatory block of the beefy session, that is greater than the one the block corresponds to
//            Optional<BigInteger> nextSessionMandatoryBlockOpt = getNextSessionMandatoryBlock(blockNumber);
//
//            // If block is greater that the biggest mandatory block or next session is the first that exists
//            if (nextSessionMandatoryBlockOpt.isEmpty()
//                    || nextSessionMandatoryBlockOpt.get().equals(beefySessions.firstEntry().getKey())) {
//
//                log.warning(String.format("No session found for the specified block: %d", blockNumber));
//                return null;
//            }
//
//            BigInteger nextSessionMandatoryBlock = nextSessionMandatoryBlockOpt.get();
//            //The beefy session, which the block corresponds to
//            Optional<Map.Entry<BigInteger, BeefySession>> currentSessionOpt = getPreviousBeefySession(
//                    nextSessionMandatoryBlock
//            );
//            if (currentSessionOpt.isEmpty()) {
//                log.warning(String.format(
//                        "No corresponding session was found for block: %d. " +
//                                "Attempted to find session preceding mandatory block: %d. ",
//                        blockNumber,
//                        nextSessionMandatoryBlock
//                ));
//                return null;
//            }
//            currentSession = currentSessionOpt.get();
//        }
//        return currentSession;
//    }

//    private Optional<BigInteger> getNextSessionMandatoryBlock(BigInteger blockNumber) {
//        return beefyState.getSessions().keySet().stream()
//                .filter(key -> key.compareTo(blockNumber) > 0)
//                .findFirst();
//    }
//
//    private Optional<Map.Entry<BigInteger, BeefySession>> getPreviousBeefySession(BigInteger beefySessionKey) {
//        Map.Entry<BigInteger, BeefySession> previousEntry = null;
//        for (Map.Entry<BigInteger, BeefySession> entry : beefyState.getSessions().entrySet()) {
//            if (entry.getKey().equals(beefySessionKey)) {
//                return Optional.ofNullable(previousEntry);
//            }
//            previousEntry = entry;
//        }
//        return Optional.empty();
//    }

    private void cleanUpSessionsAndRoundsOnJustification(BeefySession currentSession,
                                                         BigInteger blockNumber) {
        List<BeefySession> beefySessions = beefyState.getSessions();
        BigInteger currentSessionMandatoryBlock = currentSession.getMandatoryBlock();
        // Remove all sessions before the current one
        beefySessions.removeIf(session -> session.getMandatoryBlock()
                .compareTo(currentSessionMandatoryBlock) < 0
        );

        if (!currentSession.equals(beefySessions.getLast())) {
            Map<Commitment, BeefyRound> currentSessionRounds = currentSession.getRounds();
            currentSessionRounds.keySet().removeIf(key -> key.getBlockNumber().compareTo(blockNumber) < 0);
        }
    }

//    /**
//     * Applied to all the blocks between beefyDigest and grandpa finalized.
//     * Each found mandatory block with its corresponding beefy session is appended.
//     */
//    private void handleBeefySessionTransitions() {
//        BigInteger nextDigest = beefyState.getNextDigest();
//        BigInteger grandpaFinalized = beefyState.getGrandpaFinalized();
//        LinkedHashMap<BigInteger, BeefySession> beefySessions = beefyState.getSessions();
//
//        if (Objects.isNull(nextDigest)) {
//            beefyState.initializeNextDigest();
//        }
//
//        if (Objects.isNull(grandpaFinalized)) {
//            throw new BeefyGenericException("Grandpa finalized is not initialized yet.");
//        }
//
//        while (nextDigest.compareTo(grandpaFinalized) <= 0) {
//            //TODO: In kagome fetching header.
//            Map<BigInteger, BeefySession> found = processConsensusMessages(
//                    nextDigest,
//                    beefySessions.isEmpty() ? beefyState.getBeefyGenesis() : nextDigest
//            );
//
//            if (!found.isEmpty()) {
//                beefySessions.putAll(found);
//            }
//
//            nextDigest = nextDigest.add(BigInteger.ONE);
//        }
//        beefyState.setNextDigest(nextDigest);
//
//        // stop voting on first finalized session when there are more sessions
//        BigInteger sessionMandatoryBlock = beefySessions.firstEntry().getKey();
//        if (beefySessions.size() > 1 && sessionMandatoryBlock.compareTo(beefyState.getBeefyFinalized()) <= 0) {
//            beefySessions.remove(sessionMandatoryBlock);
//        }
//
//        //TODO: Start voting
//
//        //TODO: Consider if we already have beefy sessions and we do not have justification
//        //      for the first mandatory block to fetch it.
//    }

    /**
     * Examines BEEFY authority consensus messages, within grandpaFinalized currently known for Beefy + 1
     * and the new finalized block from Grandpa. It detects authority set changes or disabled authorities.
     * <p>
     * Upon encountering BEEFY_CHANGED_AUTHORITIES message, it finds keyPair, based on public keys,
     * and extracts the authority set. New BeefySession is created and added to the collection.
     * <p>
     * If a BEEFY_ON_DISABLED message is found, it updates the beefyState with the disabled authority information.
     */
    private void processConsensusMessages(List<BlockHeader> headers) {
        BigInteger grandpaFinalized = beefyState.getGrandpaFinalized();
        if (Objects.isNull(grandpaFinalized)) {
            throw new BeefyGenericException("Grandpa finalized is not initialized yet.");
        }

        BigInteger firstBlockNumber = headers.getFirst().getBlockNumber();
        if (!firstBlockNumber.equals(grandpaFinalized.add(BigInteger.ONE))) {
            throw new BeefyGenericException("First new block for BEEFY should be exactly 1 " +
                    "greater than its currently known grandpaFinalized.");
        }

        for (BlockHeader currentHeader : headers) {
            DigestHelper.getBeefyConsensusMessages(currentHeader.getDigest())
                    .forEach(cm -> stateManager.getBeefyState().handleBeefyConsensusMessage(
                            cm, currentHeader.getBlockNumber())
                    );
        }
    }

    private Commitment getCommitment(BigInteger blockNumber, BigInteger setId) {
        BlockState blockState = stateManager.getBlockState();
        BlockHeader blockHeader = null;
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
}
