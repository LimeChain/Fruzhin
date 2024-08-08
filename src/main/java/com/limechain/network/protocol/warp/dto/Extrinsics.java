package com.limechain.network.protocol.warp.dto;

import com.limechain.exception.scale.ScaleEncodingException;
import com.limechain.polkaj.writer.ScaleCodecWriter;
import lombok.Data;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Data
public class Extrinsics {

    private final byte[] extrinsic;

    @Override
    public String toString() {
        return null;// Hex.toHexString(extrinsic);
    }

    public byte[] getHash() {
        try (ByteArrayOutputStream buf = new ByteArrayOutputStream();
             ScaleCodecWriter writer = new ScaleCodecWriter(buf)) {
            writer.writeAsList(extrinsic);
            return null;//HashUtils.hashWithBlake2b(buf.toByteArray());
        } catch (IOException e) {
            throw new ScaleEncodingException(e);
        }
    }

}
