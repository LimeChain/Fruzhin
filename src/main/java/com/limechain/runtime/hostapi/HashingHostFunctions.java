package com.limechain.runtime.hostapi;

import com.limechain.utils.HashUtils;
import lombok.AllArgsConstructor;
import org.wasmer.ImportObject;
import org.wasmer.Type;

import java.util.Arrays;
import java.util.List;

/**
 * Implementations of the Hashing HostAPI functions
 * For more info check
 * {<a href="https://spec.polkadot.network/chap-host-api#sect-hashing-api">Hashing API</a>}
 */
@AllArgsConstructor
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
     * @param data a pointer-size to the data to be hashed.
     * @return a pointer to the buffer containing the 256-bit hash result.
     */
    public int keccak256V1(RuntimePointerSize data) {
        byte[] dataToHash = hostApi.getDataFromMemory(data);

        byte[] hash = HashUtils.hashWithKeccak256(dataToHash);

        return hostApi.writeDataToMemory(hash).pointer();
    }

    /**
     * Conducts a 512-bit Keccak hash.
     *
     * @param data a pointer-size to the data to be hashed.
     * @return a pointer to the buffer containing the 512-bit hash result.
     */
    public int keccak512V1(RuntimePointerSize data) {
        byte[] dataToHash = hostApi.getDataFromMemory(data);

        byte[] hash = HashUtils.hashWithKeccak512(dataToHash);

        return hostApi.writeDataToMemory(hash).pointer();
    }

    /**
     * Conducts a 256-bit SHA2 hash.
     *
     * @param data a pointer-size to the data to be hashed.
     * @return a pointer to the buffer containing the 256-bit hash result.
     */
    public int sha2256V1(RuntimePointerSize data) {
        byte[] dataToHash = hostApi.getDataFromMemory(data);

        byte[] hash = HashUtils.hashWithSha256(dataToHash);

        return hostApi.writeDataToMemory(hash).pointer();
    }

    /**
     * Conducts a 128-bit Blake2 hash.
     *
     * @param data a pointer-size to the data to be hashed.
     * @return a pointer to the buffer containing the 128-bit hash result.
     */
    public int blake2128V1(RuntimePointerSize data) {
        byte[] dataToHash = hostApi.getDataFromMemory(data);

        byte[] hash = HashUtils.hashWithBlake2b128(dataToHash);

        return hostApi.writeDataToMemory(hash).pointer();
    }

    /**
     * Conducts a 256-bit Blake2 hash.
     *
     * @param data a pointer-size to the data to be hashed.
     * @return a pointer to the buffer containing the 256-bit hash result.
     */
    public int blake2256V1(RuntimePointerSize data) {
        byte[] dataToHash = hostApi.getDataFromMemory(data);

        byte[] hash = HashUtils.hashWithBlake2b(dataToHash);

        return hostApi.writeDataToMemory(hash).pointer();
    }

    /**
     * Conducts a 64-bit xxHash hash.
     *
     * @param data a pointer-size to the data to be hashed.
     * @return a pointer to the buffer containing the 64-bit hash result.
     */
    public int twox64V1(final RuntimePointerSize data) {
        byte[] dataToHash = hostApi.getDataFromMemory(data);

        byte[] hash = HashUtils.hashXx64(0, dataToHash);

        return hostApi.writeDataToMemory(hash).pointer();
    }

    /**
     * Conducts a 128-bit xxHash hash.
     *
     * @param data a pointer-size to the data to be hashed.
     * @return a pointer to the buffer containing the 128-bit hash result.
     */
    public int twox128V1(final RuntimePointerSize data) {
        byte[] dataToHash = hostApi.getDataFromMemory(data);

        byte[] hash = HashUtils.hashXx128(0, dataToHash);

        return hostApi.writeDataToMemory(hash).pointer();
    }

    /**
     * Conducts a 256-bit xxHash hash.
     *
     * @param data a pointer-size to the data to be hashed.
     * @return a pointer to the buffer containing the 256-bit hash result.
     */
    public int twox256V1(final RuntimePointerSize data) {
        byte[] dataToHash = hostApi.getDataFromMemory(data);

        byte[] hash = HashUtils.hashXx256(0, dataToHash);

        return hostApi.writeDataToMemory(hash).pointer();
    }

}
