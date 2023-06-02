package com.limechain.internal;

import com.limechain.utils.LittleEndianUtils;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;

public class DecodeLeaf {
    public static byte[] decodeKey(ScaleCodecReader reader, int partialKeyLength) throws TrieDecoderException {
        try {
            if (partialKeyLength == 0) {
                return new byte[]{};
            }

            int keySize = partialKeyLength / 2 + partialKeyLength % 2;
            byte[] key = reader.readByteArray();
            if (keySize != key.length) {
                throw new TrieDecoderException("Read bytes is not equal to key size. Read " +
                        key + " bytes, expected " + key.length);
            }
            return LittleEndianUtils.convertBytes(key);
        } catch (IndexOutOfBoundsException error) {
            throw new TrieDecoderException("Could not decode partial key: " + error.getMessage());
        }
    }
}
