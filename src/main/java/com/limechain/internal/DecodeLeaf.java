package com.limechain.internal;

import com.limechain.utils.LittleEndianUtils;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;

public class DecodeLeaf {
    public static byte[] decodeKey(ScaleCodecReader reader, int partialKeyLength) {
        if (partialKeyLength == 0) {
            return new byte[]{};
        }

        int keySize = partialKeyLength / 2 + partialKeyLength % 2;
        byte[] key = reader.readByteArray();
        if (keySize != key.length) {
            throw new IllegalStateException("Read bytes is not equal to key size. Read " +
                    key + " bytes, expected " + key.length);
        }
        return LittleEndianUtils.convertBytes(key);
    }
}
