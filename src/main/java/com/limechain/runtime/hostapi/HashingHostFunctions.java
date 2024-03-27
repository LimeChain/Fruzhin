package com.limechain.runtime.hostapi;

import com.limechain.runtime.Runtime;
import com.limechain.runtime.hostapi.dto.RuntimePointerSize;
import com.limechain.utils.HashUtils;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.java.Log;
import org.wasmer.ImportObject;
import org.wasmer.Type;

import java.util.Arrays;
import java.util.List;

/**
 * Implementations of the Hashing HostAPI functions
 * For more info check
 * {<a href="https://spec.polkadot.network/chap-host-api#sect-hashing-api">Hashing API</a>}
 */
@Log
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class HashingHostFunctions {

    private final Runtime runtime;

    public static List<ImportObject> getFunctions(final Runtime runtime) {
        return new HashingHostFunctions(runtime).buildFunctions();
    }

    public List<ImportObject> buildFunctions() {
        return Arrays.asList(
                HostApi.getImportObject("ext_hashing_keccak_256_version_1", argv ->
                        keccak256V1(new RuntimePointerSize(argv.get(0))), List.of(Type.I64), Type.I32),
                HostApi.getImportObject("ext_hashing_keccak_512_version_1", argv ->
                        keccak512V1(new RuntimePointerSize(argv.get(0))), List.of(Type.I64), Type.I32),
                HostApi.getImportObject("ext_hashing_sha2_256_version_1", argv ->
                        sha2256V1(new RuntimePointerSize(argv.get(0))), List.of(Type.I64), Type.I32),
                HostApi.getImportObject("ext_hashing_blake2_128_version_1", argv ->
                        blake2128V1(new RuntimePointerSize(argv.get(0))), List.of(Type.I64), Type.I32),
                HostApi.getImportObject("ext_hashing_blake2_256_version_1", argv ->
                        blake2256V1(new RuntimePointerSize(argv.get(0))), List.of(Type.I64), Type.I32),
                HostApi.getImportObject("ext_hashing_twox_64_version_1", argv ->
                        twox64V1(new RuntimePointerSize(argv.get(0))), List.of(Type.I64), Type.I32),
                HostApi.getImportObject("ext_hashing_twox_128_version_1", argv ->
                        twox128V1(new RuntimePointerSize(argv.get(0))), List.of(Type.I64), Type.I32),
                HostApi.getImportObject("ext_hashing_twox_256_version_1", argv ->
                        twox256V1(new RuntimePointerSize(argv.get(0))), List.of(Type.I64), Type.I32));
    }

    /**
     * Conducts a 256-bit Keccak hash.
     *
     * @param data a pointer-size to the data to be hashed.
     * @return a pointer to the buffer containing the 256-bit hash result.
     */
    public int keccak256V1(RuntimePointerSize data) {
        log.info("keccak256V1");
        byte[] dataToHash = runtime.getDataFromMemory(data);

        byte[] hash = HashUtils.hashWithKeccak256(dataToHash);

        return runtime.writeDataToMemory(hash).pointer();
    }

    /**
     * Conducts a 512-bit Keccak hash.
     *
     * @param data a pointer-size to the data to be hashed.
     * @return a pointer to the buffer containing the 512-bit hash result.
     */
    public int keccak512V1(RuntimePointerSize data) {
        log.info("keccak512V1");

        byte[] dataToHash = runtime.getDataFromMemory(data);

        byte[] hash = HashUtils.hashWithKeccak512(dataToHash);

        return runtime.writeDataToMemory(hash).pointer();
    }

    /**
     * Conducts a 256-bit SHA2 hash.
     *
     * @param data a pointer-size to the data to be hashed.
     * @return a pointer to the buffer containing the 256-bit hash result.
     */
    public int sha2256V1(RuntimePointerSize data) {
        log.info("sha2256V1");

        byte[] dataToHash = runtime.getDataFromMemory(data);

        byte[] hash = HashUtils.hashWithSha256(dataToHash);

        return runtime.writeDataToMemory(hash).pointer();
    }

    /**
     * Conducts a 128-bit Blake2 hash.
     *
     * @param data a pointer-size to the data to be hashed.
     * @return a pointer to the buffer containing the 128-bit hash result.
     */
    public int blake2128V1(RuntimePointerSize data) {
        byte[] dataToHash = runtime.getDataFromMemory(data);

        byte[] hash = HashUtils.hashWithBlake2b128(dataToHash);

        return runtime.writeDataToMemory(hash).pointer();
    }

    /**
     * Conducts a 256-bit Blake2 hash.
     *
     * @param data a pointer-size to the data to be hashed.
     * @return a pointer to the buffer containing the 256-bit hash result.
     */
    public int blake2256V1(RuntimePointerSize data) {
        log.info("blake2256V1");

        byte[] dataToHash = runtime.getDataFromMemory(data);

        byte[] hash = HashUtils.hashWithBlake2b(dataToHash);

        return runtime.writeDataToMemory(hash).pointer();
    }

    /**
     * Conducts a 64-bit xxHash hash.
     *
     * @param data a pointer-size to the data to be hashed.
     * @return a pointer to the buffer containing the 64-bit hash result.
     */
    public int twox64V1(final RuntimePointerSize data) {
        log.info("twox64V1");

        byte[] dataToHash = runtime.getDataFromMemory(data);

        byte[] hash = HashUtils.hashXx64(0, dataToHash);

        return runtime.writeDataToMemory(hash).pointer();
    }

    /**
     * Conducts a 128-bit xxHash hash.
     *
     * @param data a pointer-size to the data to be hashed.
     * @return a pointer to the buffer containing the 128-bit hash result.
     */
    public int twox128V1(final RuntimePointerSize data) {
        log.info("twox128V1");

        byte[] dataToHash = runtime.getDataFromMemory(data);

        byte[] hash = HashUtils.hashXx128(0, dataToHash);

        log.info("twox128V1 hash: " + Arrays.toString(hash));
        return runtime.writeDataToMemory(hash).pointer();
    }

    /**
     * Conducts a 256-bit xxHash hash.
     *
     * @param data a pointer-size to the data to be hashed.
     * @return a pointer to the buffer containing the 256-bit hash result.
     */
    public int twox256V1(final RuntimePointerSize data) {
        log.info("twox256V1");

        byte[] dataToHash = runtime.getDataFromMemory(data);

        byte[] hash = HashUtils.hashXx256(0, dataToHash);

        return runtime.writeDataToMemory(hash).pointer();
    }

}
