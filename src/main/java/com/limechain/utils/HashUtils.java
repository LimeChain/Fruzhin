package com.limechain.utils;

import io.emeraldpay.polkaj.types.Hash256;
import lombok.experimental.UtilityClass;
import net.openhft.hashing.LongHashFunction;
import org.bouncycastle.crypto.digests.Blake2bDigest;
import org.bouncycastle.jcajce.provider.digest.Keccak;
import org.bouncycastle.jcajce.provider.digest.SHA256;
import org.web3j.crypto.Hash;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

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
     * Conducts a 256-bit Keccak hash.
     * @param input the data to be hashed.
     * @return byte array containing the 256-bit hash result.
     */
    public static byte[] hashWithKeccak256(byte[] input) {
        return Hash.sha3(input);
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

    /**
     * Conducts a 64-bit xxHash hash.
     * @param seed the seed to use for the hash. Default 0.
     * @param dataToHash the data to be hashed.
     * @return byte array containing the 64-bit hash result.
     */
    public static byte[] hashXx64(int seed, byte[] dataToHash) {
        final long xxHash = LongHashFunction
                .xx(seed)
                .hashBytes(dataToHash);

        final ByteBuffer buffer = ByteBuffer
                .allocate(HASH_64_SIZE_BYTES)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putLong(xxHash);

        return buffer.array();
    }

    /**
     * Conducts a 128-bit xxHash hash.
     * @param seed the seed to use for the hash. Default 0.
     * @param dataToHash the data to be hashed.
     * @return byte array containing the 128-bit hash result.
     */
    public static byte[] hashXx128(int seed, byte[] dataToHash) {
        byte[] hash0 = hashXx64(seed, dataToHash);
        byte[] hash1 = hashXx64(seed + 1, dataToHash);

        ByteBuffer buffer = ByteBuffer.allocate(HASH_128_SIZE_BYTES);
        buffer.put(hash0);
        buffer.put(hash1);

        return buffer.array();
    }

    /**
     * Conducts a 256-bit xxHash hash.
     * @param seed the seed to use for the hash. Default 0.
     * @param dataToHash the data to be hashed.
     * @return byte array containing the 256-bit hash result.
     */
    public static byte[] hashXx256(int seed, byte[] dataToHash) {
        byte[] hash0 = hashXx64(seed, dataToHash);
        byte[] hash1 = hashXx64(seed + 1, dataToHash);
        byte[] hash2 = hashXx64(seed + 2, dataToHash);
        byte[] hash3 = hashXx64(seed + 3, dataToHash);

        ByteBuffer buffer = ByteBuffer.allocate(Hash256.SIZE_BYTES);
        buffer.put(hash0);
        buffer.put(hash1);
        buffer.put(hash2);
        buffer.put(hash3);

        return buffer.array();
    }
}
