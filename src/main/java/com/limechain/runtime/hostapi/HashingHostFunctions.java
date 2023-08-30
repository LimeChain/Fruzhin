package com.limechain.runtime.hostapi;

import org.wasmer.ImportObject;
import org.wasmer.Type;

import java.util.Arrays;
import java.util.List;

public class HashingHostFunctions {

    public static List<ImportObject> getFunctions() {
        return Arrays.asList(new ImportObject.FuncImport("env", "ext_hashing_keccak_256_version_1", argv -> {
                    System.out.println("Message printed in the body of 'ext_hashing_keccak_256_version_1'");
                    return argv;
                }, List.of(Type.I64), List.of(Type.I32)),
                new ImportObject.FuncImport("env", "ext_hashing_keccak_512_version_1", argv -> {
                    System.out.println("Message printed in the body of 'ext_hashing_keccak_512_version_1'");
                    return argv;
                }, List.of(Type.I64), List.of(Type.I32)),
                new ImportObject.FuncImport("env", "ext_hashing_sha2_256_version_1", argv -> {
                    System.out.println("Message printed in the body of 'ext_hashing_sha2_256_version_1'");
                    return argv;
                }, List.of(Type.I64), List.of(Type.I32)),
                new ImportObject.FuncImport("env", "ext_hashing_blake2_128_version_1", argv -> {
                    System.out.println("Message printed in the body of 'ext_hashing_blake2_128_version_1'");
                    return argv;
                }, List.of(Type.I64), List.of(Type.I32)),   // Unknown import?
                new ImportObject.FuncImport("env", "ext_hashing_blake2_256_version_1", argv -> {
                    System.out.println("Message printed in the body of 'ext_hashing_blake2_256_version_1'");
                    return argv;
                }, List.of(Type.I64), List.of(Type.I32)),
                new ImportObject.FuncImport("env", "ext_hashing_twox_64_version_1", argv -> {
                    System.out.println("Message printed in the body of 'ext_hashing_twox_64_version_1'");
                    return List.of(HostApi.extHashingTwox64Version1((long) argv.get(0)));

                }, List.of(Type.I64), List.of(Type.I32)),
                new ImportObject.FuncImport("env", "ext_hashing_twox_128_version_1", argv -> {
                    System.out.println("Message printed in the body of 'ext_hashing_twox_128_version_1'");
                    return List.of(HostApi.extHashingTwox128Version1((long) argv.get(0)));
                }, List.of(Type.I64), List.of(Type.I32)),
                new ImportObject.FuncImport("env", "ext_hashing_twox_256_version_1", argv -> {
                    System.out.println("Message printed in the body of 'ext_hashing_twox_256_version_1'");
                    return List.of(HostApi.extHashingTwox256Version1((long) argv.get(0)));
                }, List.of(Type.I64), List.of(Type.I32)));
    }

}
