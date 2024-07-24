package com.limechain.network.protocol.warp.dto;

import com.limechain.exception.scale.ScaleEncodingException;
import com.limechain.utils.HashUtils;
import io.emeraldpay.polkaj.scale.ScaleCodecWriter;
import lombok.Data;
import org.bouncycastle.util.encoders.Hex;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Data
public class Extrinsics {

    private final byte[] extrinsic;

    @Override
    public String toString() {
        return Hex.toHexString(extrinsic);
    }

    public byte[] getHash() {
        try (ByteArrayOutputStream buf = new ByteArrayOutputStream();
             ScaleCodecWriter writer = new ScaleCodecWriter(buf)) {
            writer.writeAsList(extrinsic);
            return HashUtils.hashWithBlake2b(buf.toByteArray());
        } catch (IOException e) {
            throw new ScaleEncodingException(e);
        }
    }

}
