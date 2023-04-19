package com.limechain.network.protocol.blockannounce.scale;

import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleReader;
import io.emeraldpay.polkaj.types.Hash256;

public class BlockHeaderScaleReader implements ScaleReader<BlockHeader> {
    @Override
    public BlockHeader read(ScaleCodecReader reader) {
        BlockHeader header = new BlockHeader();
        header.parentHash = new Hash256(reader.readUint256());
        header.blockNumber = reader.readCompactInt();
        header.stateRoot = new Hash256(reader.readUint256());
        header.extrinsicRoot = new Hash256(reader.readUint256());
        int digests = reader.readCompactInt();
        for (var i = 0; i < digests; i++) {
            // TODO: Read digests
        }
        return header;
    }
}
