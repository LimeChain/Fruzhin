package com.limechain.network.protocol.warp.dto;

import com.limechain.utils.HashUtils;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleCodecWriter;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

@Data
@AllArgsConstructor
public class BlockBody {

    private List<Extrinsics> extrinsics;

    public byte[][] getExtrinsicsAsByteArray() {
        return extrinsics.stream()
                .map(Extrinsics::getExtrinsic)
                .toArray(byte[][]::new);
    }

    public byte[] getEncoded(){
        try (ByteArrayOutputStream buf = new ByteArrayOutputStream();
             ScaleCodecWriter writer = new ScaleCodecWriter(buf)) {

            writer.writeCompact(extrinsics.size());
            for (Extrinsics extrinsics : this.extrinsics) {
                writer.writeAsList(extrinsics.getExtrinsic());
            }

            return HashUtils.hashWithBlake2b(buf.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static BlockBody fromEncoded(byte[] encoded) {
        ScaleCodecReader reader = new ScaleCodecReader(encoded);

        List<Extrinsics> extrinsics = new LinkedList<>();

        int extrinsicsCount = reader.readCompactInt();
        for (int i = 0; i < extrinsicsCount; i++) {
            byte[] extrinsic = reader.readByteArray();
            extrinsics.add(new Extrinsics(extrinsic));
        }

        return new BlockBody(extrinsics);
    }

}
