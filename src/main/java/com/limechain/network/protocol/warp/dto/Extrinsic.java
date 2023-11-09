package com.limechain.network.protocol.warp.dto;

import com.limechain.utils.HashUtils;
import io.emeraldpay.polkaj.scale.ScaleCodecWriter;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.tomcat.util.buf.HexUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Data
@AllArgsConstructor
public class Extrinsic {
    private byte[] extrinsic;

    @Override
    public String toString(){
        return HexUtils.toHexString(extrinsic);
    }

    public byte[] getHash(){
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        try (ScaleCodecWriter writer = new ScaleCodecWriter(buf)) {
            writer.writeAsList(extrinsic);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return HashUtils.hashWithBlake2b(buf.toByteArray());
    }

}
