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

    public static byte[] hashWithBlake2b(byte[] input) {
        Blake2bDigest digest = new Blake2bDigest(HASH256_HASH_LENGTH);
        digest.reset();
        digest.update(input, 0, input.length);
        byte[] hash = new byte[digest.getDigestSize()];
        digest.doFinal(hash, 0);
        return hash;
    }

    public static byte[] hashWithBlake2b128(byte[] input) {
        return hashWithBlake2bToLength(input, 16);
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

    public static byte[] hashWithSha256(byte[] input) {
        SHA256.Digest digest = new SHA256.Digest();
        return digest.digest(input);
    }

    public static byte[] hashXx64(int seed, byte[] dataToHash) {
        final long xxHash = LongHashFunction
                .xx(seed)
                .hashBytes(dataToHash);

        final ByteBuffer buffer = ByteBuffer
                .allocate(8)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putLong(xxHash);

        return buffer.array();
    }

    public static byte[] hashXx128(int seed, byte[] dataToHash) {
        byte[] hash0 = hashXx64(seed, dataToHash);
        byte[] hash1 = hashXx64(seed + 1, dataToHash);

        ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.put(hash0);
        buffer.put(hash1);

        return buffer.array();
    }

    public static byte[] hashXx256(int seed, byte[] dataToHash) {
        byte[] hash0 = hashXx64(seed, dataToHash);
        byte[] hash1 = hashXx64(seed + 1, dataToHash);
        byte[] hash2 = hashXx64(seed + 2, dataToHash);
        byte[] hash3 = hashXx64(seed + 3, dataToHash);

        ByteBuffer buffer = ByteBuffer.allocate(32);
        buffer.put(hash0);
        buffer.put(hash1);
        buffer.put(hash2);
        buffer.put(hash3);

        return buffer.array();
    }
}
