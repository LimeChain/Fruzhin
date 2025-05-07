package com.limechain.network.protocol.blockannounce.scale;

import com.limechain.network.protocol.warp.dto.BlockHeader;
import com.limechain.network.protocol.warp.dto.DigestType;
import com.limechain.network.protocol.warp.dto.HeaderDigest;
import io.emeraldpay.polkaj.scale.ScaleCodecWriter;
import io.emeraldpay.polkaj.scale.ScaleWriter;
import io.emeraldpay.polkaj.scale.writer.ListWriter;
import io.emeraldpay.polkaj.types.Hash256;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BlockHeaderScaleWriter implements ScaleWriter<BlockHeader> {

    private static final BlockHeaderScaleWriter INSTANCE = new BlockHeaderScaleWriter();

    private final HeaderDigestScaleWriter headerDigestScaleWriter = HeaderDigestScaleWriter.getInstance();

    public static BlockHeaderScaleWriter getInstance() {
        return INSTANCE;
    }

    @Override
    public void write(ScaleCodecWriter writer, BlockHeader blockHeader) throws IOException {
        write(writer, blockHeader, true);
    }

    public void writeUnsealed(ScaleCodecWriter writer, BlockHeader blockHeader) throws IOException {
        write(writer, blockHeader, false);
    }

    private void write(ScaleCodecWriter writer, BlockHeader blockHeader, boolean sealed) throws IOException {
        writer.writeUint256(blockHeader.getParentHash().getBytes());
        // NOTE: Usage of BlockNumberWriter is intentionally omitted here,
        //  since we want this to be a compact int, not a var size int
        writer.writeCompact(blockHeader.getBlockNumber().intValueExact());

        writeHashOrEmpty(writer, blockHeader.getStateRoot());
        writeHashOrEmpty(writer, blockHeader.getExtrinsicsRoot());

        List<HeaderDigest> digestItems = getFilteredDigests(blockHeader, sealed);
        new ListWriter<>(headerDigestScaleWriter).write(writer, digestItems);
    }

    private void writeHashOrEmpty(ScaleCodecWriter writer, Hash256 hash) throws IOException {
        byte[] bytes = (hash != null) ? hash.getBytes() : new byte[32];
        writer.writeUint256(bytes);
    }

    private List<HeaderDigest> getFilteredDigests(BlockHeader blockHeader, boolean sealed) {

        if (blockHeader.getDigest() == null || blockHeader.getDigest().length == 0) {
            return Collections.emptyList();
        }

        return Arrays.stream(blockHeader.getDigest())
                .filter(digest -> sealed || digest.getType() != DigestType.SEAL)
                .toList();
    }
}
