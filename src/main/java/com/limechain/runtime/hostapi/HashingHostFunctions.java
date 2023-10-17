package com.limechain.runtime.hostapi;

import com.limechain.utils.HashUtils;
import net.openhft.hashing.LongHashFunction;
import org.bouncycastle.jcajce.provider.digest.SHA256;
import org.wasmer.ImportObject;
import org.wasmer.Type;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.List;

/**
 * Implementations of the Hashing HostAPI functions
 * For more info check
 * {<a href="https://spec.polkadot.network/chap-host-api#sect-hashing-api">Hashing API</a>}
 */
public class HashingHostFunctions {

    private final HostApi hostApi;

    private HashingHostFunctions() {
        this.hostApi = HostApi.getInstance();
    }

    public static List<ImportObject> getFunctions() {
        return new HashingHostFunctions().buildFunctions();
    }

    public List<ImportObject> buildFunctions() {
        return Arrays.asList(
                HostApi.getImportObject("ext_hashing_keccak_256_version_1", argv ->
                        List.of(keccak256V1(new RuntimePointerSize(argv.get(0)))), List.of(Type.I64), Type.I32),
                HostApi.getImportObject("ext_hashing_keccak_512_version_1", argv ->
                        List.of(keccak512V1(new RuntimePointerSize(argv.get(0)))), List.of(Type.I64), Type.I32),
                HostApi.getImportObject("ext_hashing_sha2_256_version_1", argv ->
                        List.of(sha2256V1(new RuntimePointerSize(argv.get(0)))), List.of(Type.I64), Type.I32),
                HostApi.getImportObject("ext_hashing_blake2_128_version_1", argv ->
                        List.of(blake2128V1(new RuntimePointerSize(argv.get(0)))), List.of(Type.I64), Type.I32),
                HostApi.getImportObject("ext_hashing_blake2_256_version_1", argv ->
                        List.of(blake2256V1(new RuntimePointerSize(argv.get(0)))), List.of(Type.I64), Type.I32),
                HostApi.getImportObject("ext_hashing_twox_64_version_1", argv ->
                        List.of(twox64V1(new RuntimePointerSize(argv.get(0)))), List.of(Type.I64), Type.I32),
                HostApi.getImportObject("ext_hashing_twox_128_version_1", argv ->
                        List.of(twox128V1(new RuntimePointerSize(argv.get(0)))), List.of(Type.I64), Type.I32),
                HostApi.getImportObject("ext_hashing_twox_256_version_1", argv ->
                        List.of(twox256V1(new RuntimePointerSize(argv.get(0)))), List.of(Type.I64), Type.I32));
    }

    /**
     * Conducts a 256-bit Keccak hash.
     *
     * @param data a pointer-size (Definition 201) to the data to be hashed.
     * @return a pointer (Definition 200) to the buffer containing the 256-bit hash result.
     */
    private int keccak256V1(RuntimePointerSize data) {
        byte[] dataToHash = hostApi.getDataFromMemory(data);

        byte[] hash = HashUtils.hashWithKeccak256(dataToHash);

        return hostApi.addDataToMemory(hash).pointer();
    }

    /**
     * Conducts a 512-bit Keccak hash.
     *
     * @param data a pointer-size (Definition 201) to the data to be hashed.
     * @return a pointer (Definition 200) to the buffer containing the 512-bit hash result.
     */
    private int keccak512V1(RuntimePointerSize data) {
        byte[] dataToHash = hostApi.getDataFromMemory(data);

        byte[] hash = HashUtils.hashWithKeccak512(dataToHash);

        return hostApi.addDataToMemory(hash).pointer();
    }

    /**
     * Conducts a 256-bit SHA2 hash.
     *
     * @param data a pointer-size (Definition 201) to the data to be hashed.
     * @return a pointer (Definition 200) to the buffer containing the 256-bit hash result.
     */
    private int sha2256V1(RuntimePointerSize data) {
        byte[] dataToHash = hostApi.getDataFromMemory(data);

        SHA256.Digest digest = new SHA256.Digest();
        byte[] hash = digest.digest(dataToHash);

        return hostApi.addDataToMemory(hash).pointer();
    }

    /**
     * Conducts a 128-bit Blake2 hash.
     *
     * @param data a pointer-size (Definition 201) to the data to be hashed.
     * @return a pointer (Definition 200) to the buffer containing the 128-bit hash result.
     */
    public int blake2128V1(RuntimePointerSize data) {
        byte[] dataToHash = hostApi.getDataFromMemory(data);

        byte[] hash = HashUtils.hashWithBlake2b128(dataToHash);

        return hostApi.addDataToMemory(hash).pointer();
    }

    /**
     * Conducts a 256-bit Blake2 hash.
     *
     * @param data a pointer-size (Definition 201) to the data to be hashed.
     * @return a pointer (Definition 200) to the buffer containing the 256-bit hash result.
     */
    public int blake2256V1(RuntimePointerSize data) {
        byte[] dataToHash = hostApi.getDataFromMemory(data);

        byte[] hash = HashUtils.hashWithBlake2b(dataToHash);

        return hostApi.addDataToMemory(hash).pointer();
    }

    /**
     * Conducts a 64-bit xxHash hash.
     *
     * @param data a pointer-size (Definition 201) to the data to be hashed.
     * @return a pointer (Definition 200) to the buffer containing the 64-bit hash result.
     */
    private int twox64V1(final RuntimePointerSize data) {
        byte[] dataToHash = hostApi.getDataFromMemory(data);
        byte[] hash0 = hash64(0, dataToHash);

        return hostApi.addDataToMemory(hash0).pointer();
    }

    /**
     * Conducts a 128-bit xxHash hash.
     *
     * @param data a pointer-size (Definition 201) to the data to be hashed.
     * @return a pointer (Definition 200) to the buffer containing the 128-bit hash result.
     */
    public int twox128V1(final RuntimePointerSize data) {
        byte[] dataToHash = hostApi.getDataFromMemory(data);

        byte[] hash0 = hash64(0, dataToHash);
        byte[] hash1 = hash64(1, dataToHash);

        ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.put(hash0);
        buffer.put(hash1);

        byte[] byteArray = buffer.array();
        return hostApi.addDataToMemory(byteArray).pointer();
    }

    /**
     * Conducts a 256-bit xxHash hash.
     *
     * @param data a pointer-size (Definition 201) to the data to be hashed.
     * @return a pointer (Definition 200) to the buffer containing the 256-bit hash result.
     */
    public int twox256V1(final RuntimePointerSize data) {
        byte[] dataToHash = hostApi.getDataFromMemory(data);

        byte[] hash0 = hash64(0, dataToHash);
        byte[] hash1 = hash64(1, dataToHash);
        byte[] hash2 = hash64(2, dataToHash);
        byte[] hash3 = hash64(3, dataToHash);

        ByteBuffer buffer = ByteBuffer.allocate(32);
        buffer.put(hash0);
        buffer.put(hash1);
        buffer.put(hash2);
        buffer.put(hash3);

        byte[] byteArray = buffer.array();
        return hostApi.addDataToMemory(byteArray).pointer();
    }

    public byte[] hash64(int seed, byte[] dataToHash) {
        final long res3 = LongHashFunction
                .xx(seed)
                .hashBytes(dataToHash);

        final ByteBuffer buffer = ByteBuffer
                .allocate(8)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putLong(res3);

        return buffer.array();
    }

}
