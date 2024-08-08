package com.limechain.storage;

import com.limechain.exception.scale.ScaleEncodingException;
import com.limechain.polkaj.writer.ScaleCodecWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public record DeleteByPrefixResult(int deleted, boolean all) {
    public byte[] scaleEncoded() {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        try (ScaleCodecWriter writer = new ScaleCodecWriter(buf)) {
            writer.writeByte(all ? 1 : 0);
            writer.writeUint32(deleted);
        } catch (IOException e) {
            throw new ScaleEncodingException(e);
        }
        return buf.toByteArray();
    }
}
