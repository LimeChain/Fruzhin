package com.limechain.runtime.hostapi;

import lombok.experimental.UtilityClass;
import org.wasmer.ImportObject;
import org.wasmer.Type;

import java.util.Arrays;
import java.util.List;

@UtilityClass
public class HashingHostFunctions {

    public static List<ImportObject> getFunctions() {
        return Arrays.asList(
                HostFunctions.getImportObject("ext_hashing_keccak_256_version_1", argv -> {
                    return argv;
                }, List.of(Type.I64), Type.I32),
                HostFunctions.getImportObject("ext_hashing_keccak_512_version_1", argv -> {
                    return argv;
                }, List.of(Type.I64), Type.I32),
                HostFunctions.getImportObject("ext_hashing_sha2_256_version_1", argv -> {
                    return argv;
                }, List.of(Type.I64), Type.I32),
                HostFunctions.getImportObject("ext_hashing_blake2_128_version_1", argv -> {
                    return argv;
                }, List.of(Type.I64), Type.I32),   // Unknown import?
                HostFunctions.getImportObject("ext_hashing_blake2_256_version_1", argv -> {
                    return argv;
                }, List.of(Type.I64), Type.I32),
                HostFunctions.getImportObject("ext_hashing_twox_64_version_1", argv -> {
                    return List.of(HostApi.extHashingTwox64Version1((long) argv.get(0)));
                }, List.of(Type.I64), Type.I32),
                HostFunctions.getImportObject("ext_hashing_twox_128_version_1", argv -> {
                    return List.of(HostApi.extHashingTwox128Version1((long) argv.get(0)));
                }, List.of(Type.I64), Type.I32),
                HostFunctions.getImportObject("ext_hashing_twox_256_version_1", argv -> {
                    return List.of(HostApi.extHashingTwox256Version1((long) argv.get(0)));
                }, List.of(Type.I64), Type.I32));
    }
}
