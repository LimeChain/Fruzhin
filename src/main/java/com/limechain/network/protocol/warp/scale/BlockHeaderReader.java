package com.limechain.network.protocol.warp.scale;

import com.limechain.network.protocol.warp.dto.BlockHeader;
import com.limechain.network.protocol.warp.dto.HeaderDigest;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleReader;
import io.emeraldpay.polkaj.types.Hash256;

import java.util.ArrayList;
import java.util.List;

public class BlockHeaderReader implements ScaleReader<BlockHeader> {
    @Override
    public BlockHeader read(ScaleCodecReader reader) {
        BlockHeader blockHeader = new BlockHeader();
        blockHeader.setParentHash(new Hash256(reader.readUint256()));
        blockHeader.setNumber(reader.readCompactInt());
        blockHeader.setStateRoot(new Hash256(reader.readUint256()));
        blockHeader.setExtrinsicsRoot(new Hash256(reader.readUint256()));
        var digestCount = reader.readCompactInt();
        List<HeaderDigest> digests = new ArrayList<>();
        for (int i = 0; i < digestCount; i++) {
            digests.add(new HeaderDigestReader().read(reader));
        }
        blockHeader.setDigest(digests.toArray(HeaderDigest[]::new));
        return blockHeader;
    }
}
