package com.limechain.network.protocol.warp.dto;

import com.limechain.network.protocol.warp.exception.ScaleEncodingException;
import com.limechain.network.protocol.warp.scale.reader.BlockBodyReader;
import com.limechain.network.protocol.warp.scale.writer.BlockBodyWriter;
import com.limechain.utils.HashUtils;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleCodecWriter;
import lombok.Data;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
        try (ByteArrayOutputStream buf = new ByteArrayOutputStream();
             ScaleCodecWriter writer = new ScaleCodecWriter(buf)) {

            BlockBodyWriter.getInstance().write(writer, this);

            return HashUtils.hashWithBlake2b(buf.toByteArray());
        } catch (IOException e) {
            throw new ScaleEncodingException(e);
        }
    }

    public static BlockBody fromEncoded(byte[] encoded) {
        ScaleCodecReader reader = new ScaleCodecReader(encoded);
        return BlockBodyReader.getInstance().read(reader);
    }

}
