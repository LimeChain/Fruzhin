package com.limechain.runtime.hostapi;

import lombok.experimental.UtilityClass;
import net.openhft.hashing.LongHashFunction;
import org.wasmer.ImportObject;
import org.wasmer.Type;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.List;

@UtilityClass
public class HashingHostFunctions {

    public static List<ImportObject> getFunctions() {
        return Arrays.asList(
                HostApi.getImportObject("ext_hashing_keccak_256_version_1", argv -> {
                    return argv;
                }, List.of(Type.I64), Type.I32),
                HostApi.getImportObject("ext_hashing_keccak_512_version_1", argv -> {
                    return argv;
                }, List.of(Type.I64), Type.I32),
                HostApi.getImportObject("ext_hashing_sha2_256_version_1", argv -> {
                    return argv;
                }, List.of(Type.I64), Type.I32),
                HostApi.getImportObject("ext_hashing_blake2_128_version_1", argv -> {
                    return argv;
                }, List.of(Type.I64), Type.I32),   // Unknown import?
                HostApi.getImportObject("ext_hashing_blake2_256_version_1", argv -> {
                    return argv;
                }, List.of(Type.I64), Type.I32),
                HostApi.getImportObject("ext_hashing_twox_64_version_1", argv -> {
                    return List.of(extHashingTwox64Version1((long) argv.get(0)));
                }, List.of(Type.I64), Type.I32),
                HostApi.getImportObject("ext_hashing_twox_128_version_1", argv -> {
                    return List.of(extHashingTwox128Version1((long) argv.get(0)));
                }, List.of(Type.I64), Type.I32),
                HostApi.getImportObject("ext_hashing_twox_256_version_1", argv -> {
                    return List.of(extHashingTwox256Version1((long) argv.get(0)));
                }, List.of(Type.I64), Type.I32));
    }

    private static int extHashingTwox64Version1(long addr) {
        byte[] dataToHash = HostApi.getDataFromMemory(addr);

        byte[] hash0 = hash64(0, dataToHash);

        return HostApi.putDataToMemory(hash0);
    }

    private static int extHashingTwox128Version1(long addr) {
        byte[] dataToHash = HostApi.getDataFromMemory(addr);

        byte[] hash0 = hash64(0, dataToHash);
        byte[] hash1 = hash64(1, dataToHash);

        ByteBuffer buffer = ByteBuffer.allocate(16);
        buffer.put(hash0);
        buffer.put(hash1);

        byte[] byteArray = buffer.array();
        return HostApi.putDataToMemory(byteArray);
    }

    private static int extHashingTwox256Version1(long addr) {
        byte[] dataToHash = HostApi.getDataFromMemory(addr);

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
        return HostApi.putDataToMemory(byteArray);
    }

    private static byte[] hash64(int seed, byte[] dataToHash) {
        final long res3 = LongHashFunction
                .xx(seed)
                .hashBytes(dataToHash.clone());

        final ByteBuffer buffer = ByteBuffer
                .allocate(8)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putLong(res3);

        return buffer.array();
    }

}
