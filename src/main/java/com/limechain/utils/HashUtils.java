package com.limechain.utils;

import io.emeraldpay.polkaj.types.Hash256;
import org.bouncycastle.crypto.digests.Blake2bDigest;

public class HashUtils {
    public static byte[] hashWithBlake2b(byte[] input) {
        Blake2bDigest digest = new Blake2bDigest(Hash256.SIZE_BYTES * Byte.SIZE);
        digest.reset();
        digest.update(input, 0, input.length);
        byte[] hash = new byte[digest.getDigestSize()];
        digest.doFinal(hash, 0);
        return hash;
    }

}
