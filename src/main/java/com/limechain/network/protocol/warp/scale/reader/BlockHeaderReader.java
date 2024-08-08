package com.limechain.network.protocol.warp.scale.reader;

import com.limechain.network.protocol.warp.dto.BlockHeader;
import com.limechain.network.protocol.warp.dto.HeaderDigest;
import com.limechain.polkaj.Hash256;
import com.limechain.polkaj.reader.ScaleCodecReader;
import com.limechain.polkaj.reader.ScaleReader;

import java.math.BigInteger;

public class BlockHeaderReader implements ScaleReader<BlockHeader> {
    @Override
    public BlockHeader read(ScaleCodecReader reader) {
        BlockHeader blockHeader = new BlockHeader();
        blockHeader.setParentHash(new Hash256(reader.readUint256()));
        // NOTE: Usage of BlockNumberReader is intentionally omitted here,
        //  since we want this to be a compact int, not a var size int
        blockHeader.setBlockNumber(BigInteger.valueOf(reader.readCompactInt()));
        blockHeader.setStateRoot(new Hash256(reader.readUint256()));
        blockHeader.setExtrinsicsRoot(new Hash256(reader.readUint256()));

        var digestCount = reader.readCompactInt();
        HeaderDigest[] digests = new HeaderDigest[digestCount];
        for (int i = 0; i < digestCount; i++) {
            digests[i] = new HeaderDigestReader().read(reader);
        }

        blockHeader.setDigest(digests);

        return blockHeader;
    }
}
