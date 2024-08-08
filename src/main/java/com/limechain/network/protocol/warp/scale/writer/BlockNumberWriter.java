package com.limechain.network.protocol.warp.scale.writer;

import com.limechain.network.protocol.warp.dto.BlockHeader;
import com.limechain.polkaj.writer.ScaleCodecWriter;
import com.limechain.polkaj.writer.ScaleWriter;

import java.io.IOException;
import java.math.BigInteger;

public class BlockNumberWriter implements ScaleWriter<BigInteger> {
    private static final BlockNumberWriter INSTANCE = new BlockNumberWriter();

    private final VarUint64Writer varUint64Writer;

    private BlockNumberWriter() {
        this.varUint64Writer = new VarUint64Writer(BlockHeader.BLOCK_NUMBER_SIZE);
    }

    public static BlockNumberWriter getInstance() {
        return INSTANCE;
    }

    @Override
    public void write(ScaleCodecWriter scaleCodecWriter, BigInteger blockNumber) throws IOException {
        this.varUint64Writer.write(scaleCodecWriter, blockNumber);
    }
}
