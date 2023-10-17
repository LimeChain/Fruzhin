package com.limechain.utils;

import io.emeraldpay.polkaj.types.Hash256;
import lombok.experimental.UtilityClass;
import org.bouncycastle.crypto.digests.Blake2bDigest;
import org.bouncycastle.jcajce.provider.digest.Keccak;
import org.web3j.crypto.Hash;

@UtilityClass
public class HashUtils {
    public static final int HASH256_HASH_LENGTH = Hash256.SIZE_BYTES * Byte.SIZE;
    public static final int HASH128_HASH_LENGTH = 16 * Byte.SIZE;
    public static byte[] hashWithBlake2b(byte[] input) {
        Blake2bDigest digest = new Blake2bDigest(HASH256_HASH_LENGTH);
        digest.reset();
        digest.update(input, 0, input.length);
        byte[] hash = new byte[digest.getDigestSize()];
        digest.doFinal(hash, 0);
        return hash;
    }

    public static byte[] hashWithBlake2b128(byte[] input) {
        Blake2bDigest digest = new Blake2bDigest(HASH128_HASH_LENGTH);
        digest.reset();
        digest.update(input, 0, input.length);
        byte[] hash = new byte[digest.getDigestSize()];
        digest.doFinal(hash, 0);
        return hash;
    }

    public static byte[] hashWithBlake2bToLength(byte[] input, int length) {
        Blake2bDigest digest = new Blake2bDigest(length * Byte.SIZE);
        digest.reset();
        digest.update(input, 0, input.length);
        byte[] hash = new byte[digest.getDigestSize()];
        digest.doFinal(hash, 0);
        return hash;
    }

    public static byte[] hashWithKeccak256(byte[] input) {
        return Hash.sha3(input);
    }

    public static byte[] hashWithKeccak512(byte[] input) {
        Keccak.DigestKeccak digest = new Keccak.Digest512();
        digest.update(input, 0, input.length);
        return digest.digest();
    }
}
