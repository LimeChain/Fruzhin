package com.limechain.runtime.hostapi;

import lombok.experimental.UtilityClass;
import org.wasmer.ImportObject;
import org.wasmer.Type;

import java.util.Arrays;
import java.util.List;

@UtilityClass
public class TrieHostFunctions {

    public static List<ImportObject> getFunctions() {
        return Arrays.asList(
                HostApi.getImportObject("ext_trie_blake2_256_root_version_1", argv -> {
                    return argv;
                }, List.of(Type.I64), Type.I32),
                HostApi.getImportObject("ext_trie_blake2_256_root_version_2", argv -> {
                    return argv;
                }, List.of(Type.I64, Type.I32), Type.I32),
                HostApi.getImportObject("ext_trie_blake2_256_ordered_root_version_1", argv -> {
                    return argv;
                }, List.of(Type.I64), Type.I32),
                HostApi.getImportObject("ext_trie_blake2_256_ordered_root_version_2", argv -> {
                    return argv;
                }, List.of(Type.I64, Type.I32), Type.I32),
                HostApi.getImportObject("ext_trie_keccak_256_root_version_1", argv -> {
                    return argv;
                }, List.of(Type.I64), Type.I32),
                HostApi.getImportObject("ext_trie_keccak_256_root_version_2", argv -> {
                    return argv;
                }, List.of(Type.I64, Type.I32), Type.I32),
                HostApi.getImportObject("ext_trie_keccak_256_ordered_root_version_1", argv -> {
                    return argv;
                }, List.of(Type.I64), Type.I32),
                HostApi.getImportObject("ext_trie_keccak_256_ordered_root_version_2", argv -> {
                    return argv;
                }, List.of(Type.I64, Type.I32), Type.I32),
                HostApi.getImportObject("ext_trie_blake2_256_verify_proof_version_1", argv -> {
                    return argv;
                }, List.of(Type.I32, Type.I64, Type.I64, Type.I64), Type.I32),
                HostApi.getImportObject("ext_trie_blake2_256_verify_proof_version_2", argv -> {
                    return argv;
                }, List.of(Type.I32, Type.I64, Type.I64, Type.I64, Type.I32), Type.I32),
                HostApi.getImportObject("ext_trie_keccak_256_verify_proof_version_1", argv -> {
                    return argv;
                }, List.of(Type.I32, Type.I64, Type.I64, Type.I64), Type.I32),
                HostApi.getImportObject("ext_trie_keccak_256_verify_proof_version_2", argv -> {
                    return argv;
                }, List.of(Type.I32, Type.I64, Type.I64, Type.I64, Type.I32), Type.I32)
        );
    }

}
