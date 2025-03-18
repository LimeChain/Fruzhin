package com.limechain.consensus.beefy;

import com.limechain.consensus.beefy.dto.BeefyAuthoritySet;
import com.limechain.consensus.beefy.dto.BeefyPayloadId;
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
