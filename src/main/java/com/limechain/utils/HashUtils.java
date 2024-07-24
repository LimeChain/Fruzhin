package com.limechain.utils;

import io.emeraldpay.polkaj.types.Hash256;
import lombok.experimental.UtilityClass;
import org.bouncycastle.crypto.digests.Blake2bDigest;
import org.bouncycastle.jcajce.provider.digest.Keccak;
import org.bouncycastle.jcajce.provider.digest.SHA256;

@UtilityClass
public class HashUtils {
    public static final int HASH256_HASH_LENGTH = Hash256.SIZE_BYTES * Byte.SIZE;
    public static final int HASH_128_SIZE_BYTES = 16;
    public static final int HASH_64_SIZE_BYTES = 8;

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

    /**
     * Conducts a 1258-bit Blake2b hash.
     * @param input the data to be hashed.
     * @return byte array containing the 128-bit hash result.
     */
    public static byte[] hashWithBlake2b128(byte[] input) {
        return hashWithBlake2bToLength(input, HASH_128_SIZE_BYTES);
    }

    /**
     * Conducts a variable bit Blake2b hash.
     * @param input the data to be hashed.
     * @param length the length of the hash in bytes.
     * @return byte array containing the hash result.
     */
    public static byte[] hashWithBlake2bToLength(byte[] input, int length) {
        Blake2bDigest digest = new Blake2bDigest(length * Byte.SIZE);
        digest.reset();
        digest.update(input, 0, input.length);
        byte[] hash = new byte[digest.getDigestSize()];
        digest.doFinal(hash, 0);
        return hash;
    }

    /**
     * Conducts a 512-bit Keccak hash.
     * @param input the data to be hashed.
     * @return byte array containing the 512-bit hash result.
     */
    public static byte[] hashWithKeccak512(byte[] input) {
        Keccak.DigestKeccak digest = new Keccak.Digest512();
        digest.update(input, 0, input.length);
        return digest.digest();
    }

    /**
     * Conducts a 256-bit SHA-2 hash.
     * @param input the data to be hashed.
     * @return byte array containing the 256-bit hash result.
     */
    public static byte[] hashWithSha256(byte[] input) {
        SHA256.Digest digest = new SHA256.Digest();
        return digest.digest(input);
    }
}
