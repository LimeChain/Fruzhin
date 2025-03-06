package com.limechain.beefy;

import com.limechain.beefy.dto.BeefyPayloadId;
import com.limechain.beefy.dto.Commitment;
import com.limechain.beefy.dto.PayloadElement;
import com.limechain.beefy.dto.ValidatorSet;
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
import java.util.Objects;
import java.util.Optional;

@Log
@Component
@RequiredArgsConstructor
public class BeefyService {

    private static final BigInteger THRESHOLD_DENOMINATOR = BigInteger.valueOf(3);

    private final StateManager stateManager;

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

    private Commitment getCommitment(BigInteger blockNumber, BigInteger setId) {
        BlockState blockState = stateManager.getBlockState();
        BlockHeader blockHeader = null;
        try {
            blockHeader = blockState.getHeaderByNumber(blockNumber);
        } catch (BlockStorageGenericException e) {
            log.warning(String.format("getCommitment: Failed to retrieve block header for block number: %d. Exception: %s",
                    blockNumber, e.getMessage()));
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
