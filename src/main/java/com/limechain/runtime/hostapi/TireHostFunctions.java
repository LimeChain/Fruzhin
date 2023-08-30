package com.limechain.runtime.hostapi;

import org.wasmer.ImportObject;
import org.wasmer.Type;

import java.util.Arrays;
import java.util.List;

public class TireHostFunctions {

    public static List<ImportObject> getFunctions() {
        return Arrays.asList(new ImportObject.FuncImport("env", "ext_trie_blake2_256_root_version_1", argv -> {
                    System.out.println("Message printed in the body of 'ext_trie_blake2_256_root_version_1'");
                    return argv;
                }, List.of(Type.I64), List.of(Type.I32)),
                new ImportObject.FuncImport("env", "ext_trie_blake2_256_root_version_2", argv -> {
                    System.out.println("Message printed in the body of 'ext_trie_blake2_256_root_version_2'");
                    return argv;
                }, List.of(Type.I64, Type.I32), List.of(Type.I32)),
                new ImportObject.FuncImport("env",
                        "ext_trie_blake2_256_ordered_root_version_1", argv -> {
                    System.out.println("Message printed in the body of 'ext_trie_blake2_256_ordered_root_version_1'");
                    return argv;
                }, List.of(Type.I64), List.of(Type.I32)),
                new ImportObject.FuncImport("env",
                        "ext_trie_blake2_256_ordered_root_version_2", argv -> {
                    System.out.println("Message printed in the body of 'ext_trie_blake2_256_ordered_root_version_2'");
                    return argv;
                }, List.of(Type.I64, Type.I32), List.of(Type.I32)),
                new ImportObject.FuncImport("env",
                        "ext_trie_keccak_256_root_version_1", argv -> {
                    System.out.println("Message printed in the body of 'ext_trie_keccak_256_root_version_1'");
                    return argv;
                }, List.of(Type.I64), List.of(Type.I32)),
                new ImportObject.FuncImport("env",
                        "ext_trie_keccak_256_root_version_2", argv -> {
                    System.out.println("Message printed in the body of 'ext_trie_keccak_256_root_version_2'");
                    return argv;
                }, List.of(Type.I64, Type.I32), List.of(Type.I32)),
                new ImportObject.FuncImport("env",
                        "ext_trie_keccak_256_ordered_root_version_1", argv -> {
                    System.out.println("Message printed in the body of 'ext_trie_keccak_256_ordered_root_version_1'");
                    return argv;
                }, List.of(Type.I64), List.of(Type.I32)),
                new ImportObject.FuncImport("env",
                        "ext_trie_keccak_256_ordered_root_version_2", argv -> {
                    System.out.println("Message printed in the body of 'ext_trie_keccak_256_ordered_root_version_2'");
                    return argv;
                }, List.of(Type.I64, Type.I32), List.of(Type.I32)),
                new ImportObject.FuncImport("env",
                        "ext_trie_blake2_256_verify_proof_version_1", argv -> {
                    System.out.println("Message printed in the body of 'ext_trie_blake2_256_verify_proof_version_1'");
                    return argv;
                }, List.of(Type.I32, Type.I64, Type.I64, Type.I64), List.of(Type.I32)),
                new ImportObject.FuncImport("env",
                        "ext_trie_blake2_256_verify_proof_version_2", argv -> {
                    System.out.println("Message printed in the body of 'ext_trie_blake2_256_verify_proof_version_2'");
                    return argv;
                }, List.of(Type.I32, Type.I64, Type.I64, Type.I64, Type.I32), List.of(Type.I32)),
                new ImportObject.FuncImport("env",
                        "ext_trie_keccak_256_verify_proof_version_1", argv -> {
                    System.out.println("Message printed in the body of 'ext_trie_keccak_256_verify_proof_version_1'");
                    return argv;
                }, List.of(Type.I32, Type.I64, Type.I64, Type.I64), List.of(Type.I32)),
                new ImportObject.FuncImport("env",
                        "ext_trie_keccak_256_verify_proof_version_2", argv -> {
                    System.out.println("Message printed in the body of 'ext_trie_keccak_256_verify_proof_version_2'");
                    return argv;
                }, List.of(Type.I32, Type.I64, Type.I64, Type.I64, Type.I32), List.of(Type.I32))
        );
    }

}
