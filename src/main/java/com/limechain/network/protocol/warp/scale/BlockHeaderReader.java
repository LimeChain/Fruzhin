package com.limechain.network.protocol.warp.scale;

import com.limechain.network.protocol.warp.dto.BlockHeader;
import com.limechain.network.protocol.warp.dto.HeaderDigest;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleReader;
import io.emeraldpay.polkaj.types.Hash256;

import java.math.BigInteger;

public class BlockHeaderReader implements ScaleReader<BlockHeader> {
    @Override
    public BlockHeader read(ScaleCodecReader reader) {
        BlockHeader blockHeader = new BlockHeader();
        blockHeader.setParentHash(new Hash256(reader.readUint256()));
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
