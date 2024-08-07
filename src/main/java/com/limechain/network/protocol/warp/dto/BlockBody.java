package com.limechain.network.protocol.warp.dto;

import com.limechain.network.protocol.warp.scale.writer.BlockBodyWriter;
import com.limechain.utils.scale.ScaleUtils;
import lombok.Data;

import java.util.List;

@Data
public class BlockBody {

    private final List<Extrinsics> extrinsics;

    public byte[][] getExtrinsicsAsByteArray() {
        return extrinsics.stream()
                .map(Extrinsics::getExtrinsic)
                .toArray(byte[][]::new);
    }

    public byte[] getEncoded(){
        byte[] encoded = ScaleUtils.Encode.encode(BlockBodyWriter.getInstance(), this);
        return null;//HashUtils.hashWithBlake2b(encoded);
    }

}
