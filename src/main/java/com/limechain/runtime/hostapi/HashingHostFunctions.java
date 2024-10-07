package com.limechain.runtime.hostapi;

import com.limechain.runtime.SharedMemory;
import com.limechain.runtime.hostapi.dto.RuntimePointerSize;
import com.limechain.utils.HashUtils;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.java.Log;
import org.wasmer.ImportObject;

import java.util.Map;

import static com.limechain.runtime.hostapi.PartialHostApi.newImportObjectPair;

/**
 * Implementations of the Hashing HostAPI functions
 * For more info check
 * {<a href="https://spec.polkadot.network/chap-host-api#sect-hashing-api">Hashing API</a>}
 */
@Log
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class HashingHostFunctions implements PartialHostApi {

    private final SharedMemory sharedMemory;

    @Override
    public Map<Endpoint, ImportObject.FuncImport> getFunctionImports() {
        return Map.ofEntries(
            newImportObjectPair(Endpoint.ext_hashing_keccak_256_version_1, argv -> {
                return keccak256V1(new RuntimePointerSize(argv.get(0)));
            }),
            newImportObjectPair(Endpoint.ext_hashing_keccak_512_version_1, argv -> {
                return keccak512V1(new RuntimePointerSize(argv.get(0)));
            }),
            newImportObjectPair(Endpoint.ext_hashing_sha2_256_version_1, argv -> {
                return sha2256V1(new RuntimePointerSize(argv.get(0)));
            }),
            newImportObjectPair(Endpoint.ext_hashing_blake2_128_version_1, argv -> {
                return blake2128V1(new RuntimePointerSize(argv.get(0)));
            }),
            newImportObjectPair(Endpoint.ext_hashing_blake2_256_version_1, argv -> {
                return blake2256V1(new RuntimePointerSize(argv.get(0)));
            }),
            newImportObjectPair(Endpoint.ext_hashing_twox_64_version_1, argv -> {
                return twox64V1(new RuntimePointerSize(argv.get(0)));
            }),
            newImportObjectPair(Endpoint.ext_hashing_twox_128_version_1, argv -> {
                return twox128V1(new RuntimePointerSize(argv.get(0)));
            }),
            newImportObjectPair(Endpoint.ext_hashing_twox_256_version_1, argv -> {
                return twox256V1(new RuntimePointerSize(argv.get(0)));
            })
        );
    }

    /**
     * Conducts a 256-bit Keccak hash.
     *
     * @param data a pointer-size to the data to be hashed.
     * @return a pointer to the buffer containing the 256-bit hash result.
     */
    public int keccak256V1(RuntimePointerSize data) {
        log.fine("keccak256V1");
        byte[] dataToHash = sharedMemory.readData(data);

        byte[] hash = HashUtils.hashWithKeccak256(dataToHash);

        return sharedMemory.writeData(hash).pointer();
    }

    /**
     * Conducts a 512-bit Keccak hash.
     *
     * @param data a pointer-size to the data to be hashed.
     * @return a pointer to the buffer containing the 512-bit hash result.
     */
    public int keccak512V1(RuntimePointerSize data) {
        log.fine("keccak512V1");

        byte[] dataToHash = sharedMemory.readData(data);

        byte[] hash = HashUtils.hashWithKeccak512(dataToHash);

        return sharedMemory.writeData(hash).pointer();
    }

    /**
     * Conducts a 256-bit SHA2 hash.
     *
     * @param data a pointer-size to the data to be hashed.
     * @return a pointer to the buffer containing the 256-bit hash result.
     */
    public int sha2256V1(RuntimePointerSize data) {
        log.fine("sha2256V1");

        byte[] dataToHash = sharedMemory.readData(data);

        byte[] hash = HashUtils.hashWithSha256(dataToHash);

        return sharedMemory.writeData(hash).pointer();
    }

    /**
     * Conducts a 128-bit Blake2 hash.
     *
     * @param data a pointer-size to the data to be hashed.
     * @return a pointer to the buffer containing the 128-bit hash result.
     */
    public int blake2128V1(RuntimePointerSize data) {
        log.fine("blake2128V1");
        byte[] dataToHash = sharedMemory.readData(data);

        byte[] hash = HashUtils.hashWithBlake2b128(dataToHash);

        return sharedMemory.writeData(hash).pointer();
    }

    /**
     * Conducts a 256-bit Blake2 hash.
     *
     * @param data a pointer-size to the data to be hashed.
     * @return a pointer to the buffer containing the 256-bit hash result.
     */
    public int blake2256V1(RuntimePointerSize data) {
        log.fine("blake2256V1");

        byte[] dataToHash = sharedMemory.readData(data);

        byte[] hash = HashUtils.hashWithBlake2b(dataToHash);

        return sharedMemory.writeData(hash).pointer();
    }

    /**
     * Conducts a 64-bit xxHash hash.
     *
     * @param data a pointer-size to the data to be hashed.
     * @return a pointer to the buffer containing the 64-bit hash result.
     */
    public int twox64V1(final RuntimePointerSize data) {
        log.fine("twox64V1");

        byte[] dataToHash = sharedMemory.readData(data);

        byte[] hash = HashUtils.hashXx64(0, dataToHash);

        return sharedMemory.writeData(hash).pointer();
    }

    /**
     * Conducts a 128-bit xxHash hash.
     *
     * @param data a pointer-size to the data to be hashed.
     * @return a pointer to the buffer containing the 128-bit hash result.
     */
    public int twox128V1(final RuntimePointerSize data) {
        log.fine("twox128V1");

        byte[] dataToHash = sharedMemory.readData(data);
        log.fine("with data to hash: " + new String(dataToHash));


        byte[] hash = HashUtils.hashXx128(0, dataToHash);

        return sharedMemory.writeData(hash).pointer();
    }

    /**
     * Conducts a 256-bit xxHash hash.
     *
     * @param data a pointer-size to the data to be hashed.
     * @return a pointer to the buffer containing the 256-bit hash result.
     */
    public int twox256V1(final RuntimePointerSize data) {
        log.fine("twox256V1");

        byte[] dataToHash = sharedMemory.readData(data);

        byte[] hash = HashUtils.hashXx256(0, dataToHash);

        return sharedMemory.writeData(hash).pointer();
    }

}
