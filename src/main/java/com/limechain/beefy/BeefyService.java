package com.limechain.beefy;

import com.limechain.beefy.dto.BeefyPayloadId;
import com.limechain.beefy.dto.Commitment;
import com.limechain.beefy.dto.PayloadElement;
import com.limechain.beefy.dto.SignedCommitment;
import com.limechain.beefy.dto.ValidatorSet;
import com.limechain.beefy.state.BeefyRound;
import com.limechain.beefy.state.BeefySession;
import com.limechain.beefy.state.BeefyState;
import com.limechain.exception.beefy.BeefyGenericException;
import com.limechain.exception.storage.BlockStorageGenericException;
import com.limechain.network.protocol.beefy.messages.consensus.BeefyConsensusMessage;
import com.limechain.network.protocol.warp.DigestHelper;
import com.limechain.network.protocol.warp.dto.BlockHeader;
import com.limechain.state.StateManager;
import com.limechain.storage.block.state.BlockState;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Log
@Component
@RequiredArgsConstructor
public class BeefyService {

    private static final BigInteger THRESHOLD_DENOMINATOR = BigInteger.valueOf(3);

    private final StateManager stateManager;
    private final BeefyState beefyState;

    /**
     * The threshold is determined as the numOfValidators - (numOfValidators - 1) / 3
     *
     * @return minimum required validators for finality.
     */
    private BigInteger getThreshold() {
        ValidatorSet validatorSet = stateManager.getBeefyState().getValidatorSet();

        if (Objects.isNull(validatorSet)) {
            log.warning("getThreshold: No validatorSet in BeefyState.");
            return BigInteger.ZERO;
        }

        var validatorSize = validatorSet.getValidators().size();

        if (validatorSize == 0) {
            log.warning("getThreshold: Validator set is empty.");
            return BigInteger.ZERO;
        }

        var numOfValidators = BigInteger.valueOf(validatorSize);
        var faulty = (numOfValidators.subtract(BigInteger.ONE)).divide(THRESHOLD_DENOMINATOR);

        return numOfValidators.subtract(faulty);
    }

    private void processJustification(SignedCommitment justification) {

        //TODO: check for justification in db

        BigInteger beefyFinalized = beefyState.getBeefyFinalized();
        if (Objects.isNull(beefyFinalized)) {
            throw new BeefyGenericException("Beefy finalized is not initialized yet.");
        }


        BigInteger blockNumber = justification.getCommitment().getBlockNumber();
        Map<BigInteger, BeefySession> found = Collections.emptyMap();

        BigInteger nextDigest = beefyState.getNextDigest();
        if (Objects.isNull(nextDigest)) {
            beefyState.initializeNextDigest();
        }

        if (blockNumber.compareTo(beefyFinalized) <= 0) {
            found = retrieveMandatoryBlockAndCreateBeefySession(blockNumber, blockNumber);
            if (found.isEmpty()) {
                return;
            }
        } else if (blockNumber.compareTo(beefyState.getNextDigest()) >= 0) {
            found = retrieveMandatoryBlockAndCreateBeefySession(blockNumber, beefyState.getNextDigest());
        }

        LinkedHashMap<BigInteger, BeefySession> beefySessions = beefyState.getSessions();
        // Initialize the current session with the last possible session that we have
        Map.Entry<BigInteger, BeefySession> currentSession = beefySessions.lastEntry();

        // If no new mandatory block and corresponding session are being found for the current block
        if (found.isEmpty()) {
            if (blockNumber.compareTo(beefyFinalized) <= 0) {
                log.warning(String.format("Block: %d has already been finalized.", blockNumber));
                return;
            }
            // The mandatory block of the beefy session, that is greater than the one the block corresponds to
            Optional<BigInteger> nextSessionMandatoryBlockOpt = getNextSessionMandatoryBlock(blockNumber);

            // If block is greater that the biggest mandatory block or next session is the first that exists
            if (nextSessionMandatoryBlockOpt.isEmpty()
                    || nextSessionMandatoryBlockOpt.get().equals(beefySessions.firstEntry().getKey())) {

                log.warning(String.format("No session found for the specified block: %d", blockNumber));
                return;
            }

            BigInteger nextSessionMandatoryBlock = nextSessionMandatoryBlockOpt.get();
            //The beefy session, which the block corresponds to
            Optional<Map.Entry<BigInteger, BeefySession>> currentSessionOpt = getPreviousBeefySession(
                    nextSessionMandatoryBlock
            );
            if (currentSessionOpt.isEmpty()) {
                log.warning(String.format(
                        "No corresponding session was found for block: %d. " +
                                "Attempted to find session preceding mandatory block: %d. ",
                        blockNumber,
                        nextSessionMandatoryBlock
                ));
                return;
            }
            currentSession = currentSessionOpt.get();
        }

        BigInteger mandatoryBlock = found.isEmpty() ?
                currentSession.getKey() : found.keySet().iterator().next();

        BeefySession beefySession = found.isEmpty() ?
                currentSession.getValue() : found.values().iterator().next();

        //TODO: Validate justification

        //TODO: Store justification in db if successfully verified

        BigInteger currentSessionMandatoryBlock = currentSession.getKey();
        // Remove all sessions before the current one
        beefySessions.keySet().removeIf(key -> key.compareTo(currentSessionMandatoryBlock) < 0);

        if (!currentSession.equals(beefySessions.lastEntry())) {
            Map<BigInteger, BeefyRound> currentSessionRounds = currentSession.getValue().getRounds();
            currentSessionRounds.keySet().removeIf(key -> key.compareTo(blockNumber) < 0);
        }

        if (!found.isEmpty()) {
            // Store the newly found block and corresponding session, where authority change appeared
            beefySessions.put(mandatoryBlock, beefySession);
        }

        //TODO: Update state
    }

    private Optional<BigInteger> getNextSessionMandatoryBlock(BigInteger blockNumber) {
        return beefyState.getSessions().keySet().stream()
                .filter(key -> key.compareTo(blockNumber) > 0)
                .findFirst();
    }

    private Optional<Map.Entry<BigInteger, BeefySession>> getPreviousBeefySession(BigInteger beefySessionKey) {
        Map.Entry<BigInteger, BeefySession> previousEntry = null;
        for (Map.Entry<BigInteger, BeefySession> entry : beefyState.getSessions().entrySet()) {
            if (entry.getKey().equals(beefySessionKey)) {
                return Optional.ofNullable(previousEntry);
            }
            previousEntry = entry;
        }
        return Optional.empty();
    }

    /**
     * Applied to all the blocks between beefyDigest and grandpa finalized.
     * Each found mandatory block with its corresponding beefy session is appended.
     */
    private void handleBeefySessionTransitions() {
        BigInteger nextDigest = beefyState.getNextDigest();
        BigInteger grandpaFinalized = beefyState.getGrandpaFinalized();
        LinkedHashMap<BigInteger, BeefySession> beefySessions = beefyState.getSessions();

        if (Objects.isNull(nextDigest)) {
            beefyState.initializeNextDigest();
        }

        if (Objects.isNull(grandpaFinalized)) {
            throw new BeefyGenericException("Grandpa finalized is not initialized yet.");
        }

        while (nextDigest.compareTo(grandpaFinalized) <= 0) {
            //TODO: In kagome fetching header.
            Map<BigInteger, BeefySession> found = retrieveMandatoryBlockAndCreateBeefySession(
                    nextDigest,
                    beefySessions.isEmpty() ? beefyState.getBeefyGenesis() : nextDigest
            );

            if (!found.isEmpty()) {
                beefySessions.putAll(found);
            }

            nextDigest = nextDigest.add(BigInteger.ONE);
        }
        beefyState.setNextDigest(nextDigest);

        // stop voting on first finalized session when there are more sessions
        BigInteger sessionMandatoryBlock = beefySessions.firstEntry().getKey();
        if (beefySessions.size() > 1 && sessionMandatoryBlock.compareTo(beefyState.getBeefyFinalized()) <= 0) {
            beefySessions.remove(sessionMandatoryBlock);
        }

        //TODO: Start voting

        //TODO: Consider if we already have beefy sessions and we do not have justification
        //      for the first mandatory block to fetch it.
    }

    /**
     * Examines BEEFY authority consensus messages, within a specified block range to detect authority set changes or
     * disabled authorities.
     * <p>
     * Upon encountering BEEFY_CHANGED_AUTHORITIES message, it extracts the authority set and returns it within a
     * BeefySession mapped to the corresponding block number. If no BEEFY_CHANGED_AUTHORITIES is found,
     * empty map is returned.
     * <p>
     * If a BEEFY_ON_DISABLED message is found, it updates the beefyState with the disabled authority information.
     */
    private Map<BigInteger, BeefySession> retrieveMandatoryBlockAndCreateBeefySession(BigInteger higherBlock,
                                                                                      BigInteger lowerBlock) {
        BlockState blockState = stateManager.getBlockState();
        BlockHeader blockHeader;
        BigInteger currentBlockNumber = higherBlock;

        while (currentBlockNumber.compareTo(lowerBlock) > 0) {

            try {
                blockHeader = blockState.getHeaderByNumber(currentBlockNumber);
            } catch (BlockStorageGenericException e) {
                log.warning(String.format("findValidators: Failed to retrieve block header for block number: %d. " +
                        "Exception: %s", higherBlock, e.getMessage()));
                currentBlockNumber = currentBlockNumber.subtract(BigInteger.ONE);
                continue;
            }

            if (Objects.isNull(beefyState.getBeefyGenesis())) {
                throw new BeefyGenericException("Beefy genesis is not initialized yet.");
            }
            if (currentBlockNumber.compareTo(beefyState.getBeefyGenesis()) <= 0) {
                // TODO: Consider implementing logic to retrieve validator set
                //  from runtime
            }

            Map<BigInteger, BeefySession> result = processAuthorityConsensusMessages(blockHeader, currentBlockNumber);
            if (!result.isEmpty()) {
                return result;
            }

            currentBlockNumber = currentBlockNumber.subtract(BigInteger.ONE);
        }

        return Collections.emptyMap();
    }

    private Map<BigInteger, BeefySession> processAuthorityConsensusMessages(BlockHeader blockHeader,
                                                                            BigInteger currentBlockNumber) {

        for (BeefyConsensusMessage consensusMessage : DigestHelper.getBeefyConsensusMessages(blockHeader.getDigest())) {
            switch (consensusMessage.getFormat()) {
                case BEEFY_CHANGED_AUTHORITIES -> {
                    ValidatorSet validatorSet = new ValidatorSet(
                            consensusMessage.getAuthorityPublicKeys(),
                            consensusMessage.getAuthoritySetId()
                    );
                    return Map.of(currentBlockNumber, new BeefySession(validatorSet));
                }
                case BEEFY_ON_DISABLED -> beefyState.setDisabledAuthority(consensusMessage.getDisabledAuthority());
            }
        }

        return Collections.emptyMap();
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
