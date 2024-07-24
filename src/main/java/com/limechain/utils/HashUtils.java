package com.limechain.utils;

import io.emeraldpay.polkaj.types.Hash256;
import lombok.experimental.UtilityClass;
import org.bouncycastle.crypto.digests.Blake2bDigest;

@UtilityClass
public class HashUtils {
    public static final int HASH256_HASH_LENGTH = Hash256.SIZE_BYTES * Byte.SIZE;

    /**
     * Conducts a 256-bit Blake2b hash.
     * @param input the data to be hashed.
     * @return byte array containing the 256-bit hash result.
     */
    public static byte[] hashWithBlake2b(byte[] input) {
        Blake2bDigest digest = new Blake2bDigest(HASH256_HASH_LENGTH);
        digest.reset();
        digest.update(input, 0, input.length);
        byte[] hash = new byte[digest.getDigestSize()];
        digest.doFinal(hash, 0);
        return hash;
    }
}
