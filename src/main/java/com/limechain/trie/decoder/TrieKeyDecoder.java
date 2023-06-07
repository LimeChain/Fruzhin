package com.limechain.trie.decoder;

import com.limechain.utils.LittleEndianUtils;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;

public class TrieKeyDecoder {
    /**
     * Decodes encoded key data for a given partial key length from a ScaleCodecReader input stream
     *
     * @param reader the ScaleCodecReader input stream used to read the encoded node data
     * @param partialKeyLength length of the partial key to be read
     * @return the decoded Node object
     * @throws TrieDecoderException if the variant does not match known variants
     */
    public static byte[] decodeKey(ScaleCodecReader reader, int partialKeyLength) throws TrieDecoderException {
        try {
            if (partialKeyLength == 0) {
                return new byte[]{};
            }

            int keySize = partialKeyLength / 2 + partialKeyLength % 2;
            byte[] key = reader.readByteArray(keySize);
            if (keySize != key.length) {
                throw new TrieDecoderException("Read bytes is not equal to key size. Read " +
                        key + " bytes, expected " + key.length);
            }
            // Maybe we will have to return only [partialKeyLength%2:]
            return LittleEndianUtils.convertBytes(key);
        } catch (IndexOutOfBoundsException error) {
            throw new TrieDecoderException("Could not decode partial key: " + error.getMessage());
        }
    }

}
