package com.limechain.network.protocol.blockannounce.scale;

import com.limechain.network.protocol.warp.dto.BlockHeader;
import com.limechain.network.protocol.warp.dto.HeaderDigest;
import io.emeraldpay.polkaj.scale.ScaleCodecWriter;
import io.emeraldpay.polkaj.scale.ScaleWriter;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.io.IOException;

@NoArgsConstructor(access  = AccessLevel.PRIVATE)
public class BlockHeaderScaleWriter implements ScaleWriter<BlockHeader> {

    private static final BlockHeaderScaleWriter INSTANCE = new BlockHeaderScaleWriter();

    private final HeaderDigestScaleWriter headerDigestScaleWriter = HeaderDigestScaleWriter.getInstance();

    public static BlockHeaderScaleWriter getInstance() {
        return INSTANCE;
    }

    @Override
    public void write(ScaleCodecWriter writer, BlockHeader blockHeader) throws IOException {
        writer.writeUint256(blockHeader.getParentHash().getBytes());
        writer.writeCompact(blockHeader.getBlockNumber().intValue());
        writer.writeUint256(blockHeader.getStateRoot().getBytes());
        writer.writeUint256(blockHeader.getExtrinsicsRoot().getBytes());

        HeaderDigest[] digests = blockHeader.getDigest();
        writer.writeCompact(digests.length);
        for (HeaderDigest digest : digests) {
            headerDigestScaleWriter.write(writer, digest);
        }
    }
}
