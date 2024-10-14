package com.limechain.transaction.dto;

import com.limechain.exception.scale.ScaleEncodingException;
import com.limechain.utils.HashUtils;
import io.emeraldpay.polkaj.scale.ScaleCodecWriter;
import lombok.Data;
import org.apache.tomcat.util.buf.HexUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Data
public class Extrinsic {

    private final byte[] data;

    @Override
    public String toString() {
        return HexUtils.toHexString(data);
    }

    public byte[] getHash() {
        try (ByteArrayOutputStream buf = new ByteArrayOutputStream();
             ScaleCodecWriter writer = new ScaleCodecWriter(buf)) {
            writer.writeAsList(data);
            return HashUtils.hashWithBlake2b(buf.toByteArray());
        } catch (IOException e) {
            throw new ScaleEncodingException(e);
        }
    }

}
