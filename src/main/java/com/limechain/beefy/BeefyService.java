package com.limechain.beefy;

import com.limechain.beefy.dto.BeefyPayloadId;
import com.limechain.beefy.dto.Commitment;
import com.limechain.beefy.dto.PayloadElement;
import com.limechain.exception.beefy.BeefyGenericException;
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

@Log
@Component
@RequiredArgsConstructor
public class BeefyService {

    private final StateManager stateManager;


    private Commitment getCommitment(BigInteger blockNumber, BigInteger setId) {
        BlockState blockState = stateManager.getBlockState();

        BlockHeader blockHeader = blockState.getHeaderByNumber(blockNumber);
        byte[] mmrHash = extractMmrRootHash(blockHeader);
        PayloadElement payloadElement = new PayloadElement(BeefyPayloadId.MMR, mmrHash);

        return new Commitment(Collections.singletonList(payloadElement), blockNumber, setId);
    }

    private byte[] extractMmrRootHash(BlockHeader blockHeader) {
        return DigestHelper.getBeefyConsensusMessages(blockHeader.getDigest())
                .stream().map(BeefyConsensusMessage::getMmrRootHash)
                .findFirst()
                .orElseThrow(() -> new BeefyGenericException(
                        String.format("No MMR digest found in block header: %d", blockHeader.getBlockNumber()))
                );
    }
}
