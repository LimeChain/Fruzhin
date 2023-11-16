package com.limechain.network.protocol.warp.dto;

import com.limechain.utils.HashUtils;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleCodecWriter;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
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
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        try (ScaleCodecWriter writer = new ScaleCodecWriter(buf)) {

            writer.writeCompact(extrinsics.size());
            for (Extrinsics extrinsics : this.extrinsics) {
                writer.writeAsList(extrinsics.getExtrinsic());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return HashUtils.hashWithBlake2b(buf.toByteArray());
    }

    public static BlockBody fromEncoded(byte[] encoded) {
        ScaleCodecReader reader = new ScaleCodecReader(encoded);

        ArrayList<Extrinsics> extrinsics = new ArrayList<>();

        int extrinsicsCount = reader.readCompactInt();
        for (int i = 0; i < extrinsicsCount; i++) {
            byte[] extrinsic = reader.readByteArray();
            extrinsics.add(new Extrinsics(extrinsic));
        }

        return new BlockBody(extrinsics);
    }

}
