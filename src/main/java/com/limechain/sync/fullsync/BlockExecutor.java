package com.limechain.sync.fullsync;

import com.limechain.network.protocol.blockannounce.scale.BlockHeaderScaleWriter;
import com.limechain.network.protocol.warp.dto.Block;
import com.limechain.network.protocol.warp.scale.reader.BlockHeaderReader;
import com.limechain.network.protocol.warp.scale.writer.BlockBodyWriter;
import com.limechain.runtime.Runtime;
import com.limechain.runtime.hostapi.dto.RuntimePointerSize;
import com.limechain.utils.scale.exceptions.ScaleEncodingException;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleCodecWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class BlockExecutor {
    private Block block;
    private Runtime runtime;

    public BlockExecutor(Block block, Runtime runtime) {
        this.block = block;
        this.runtime = runtime;
    }

    public void validateBlock() {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             var writer = new ScaleCodecWriter(baos)) {
            var blockHeaderScaleWriter = BlockHeaderScaleWriter.getInstance();
            blockHeaderScaleWriter.writeUnsealed(writer, block.getHeader());

            var blockBodyScaleWriter = BlockBodyWriter.getInstance();
            blockBodyScaleWriter.write(writer, block.getBody());
            var result = runtime.callWithArgs("Core_execute_block", runtime.writeDataToMemory(baos.toByteArray()));
            System.out.println(result != null && result.length != 0);
        } catch (IOException e) {
            throw new ScaleEncodingException(e);
        }
//        block.setHeader();
    }
}
