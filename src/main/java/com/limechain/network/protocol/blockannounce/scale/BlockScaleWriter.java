package com.limechain.network.protocol.blockannounce.scale;

import com.limechain.network.protocol.warp.dto.Block;
import io.emeraldpay.polkaj.scale.ScaleCodecWriter;
import io.emeraldpay.polkaj.scale.ScaleWriter;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.io.IOException;

@NoArgsConstructor(access  = AccessLevel.PRIVATE)
public class BlockScaleWriter implements ScaleWriter<Block> {
    private static final BlockScaleWriter INSTANCE = new BlockScaleWriter();

    @Override
    public void write(ScaleCodecWriter scaleCodecWriter, Block block) throws IOException {

    }
}
