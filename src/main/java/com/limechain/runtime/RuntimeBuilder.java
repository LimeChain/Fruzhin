package com.limechain.runtime;

import com.github.luben.zstd.Zstd;
import com.limechain.utils.ByteArrayUtils;
import org.wasmer.ImportObject;
import org.wasmer.Imports;
import org.wasmer.Module;
import org.wasmer.Type;
import lombok.extern.java.Log;

import java.util.Arrays;

@Log
public class RuntimeBuilder {
    public static final byte[] ZSTD_PREFIX = new byte[]{82, -68, 83, 118, 70, -37, -114, 5};
    public static final int MAX_ZSTD_DECOMPRESSED_SIZE = 50 * 1024 * 1024;
    public static final int DEFAULT_HEAP_PAGES = 2048;

    public static Runtime buildRuntime(byte[] code) {

        byte[] wasmBinary;
        byte[] wasmBinaryPrefix = Arrays.copyOfRange(code, 0, 8);
        if (Arrays.equals(wasmBinaryPrefix, ZSTD_PREFIX)) {
            wasmBinary = Zstd.decompress(Arrays.copyOfRange(
                    code, ZSTD_PREFIX.length, code.length), MAX_ZSTD_DECOMPRESSED_SIZE);
        } else wasmBinary = code;

        RuntimeVersion runtimeVersion = getRuntimeVersion(wasmBinary);
        Module module = new Module(wasmBinary);

        return new Runtime(module, DEFAULT_HEAP_PAGES, runtimeVersion);
    }

    private static RuntimeVersion getRuntimeVersion(byte[] wasmBinary) {
        // byte value of \0asm concatenated with 0x1, 0x0, 0x0, 0x0 from smoldot runtime_version.rs#97
        byte[] searchKey = new byte[]{0x00, 0x61, 0x73, 0x6D, 0x1, 0x0, 0x0, 0x0};

        int searchedKeyIndex = ByteArrayUtils.indexOf(wasmBinary, searchKey);
        if (searchedKeyIndex < 0) throw new RuntimeException("Key not found in runtime code");
        WasmSections wasmSections = new WasmSections();
        wasmSections.parseCustomSections(wasmBinary);
        if (wasmSections.getRuntimeVersion() != null && wasmSections.getRuntimeVersion().getRuntimeApis() != null) {
            return wasmSections.getRuntimeVersion();
        } else throw new RuntimeException("Could not get Runtime version");
    }

    static Imports getImports(Module module, int minPages) {
        ImportObject.MemoryImport memory= new ImportObject.MemoryImport("env", 22, false);
        return Imports.from(Arrays.asList(
                new ImportObject.FuncImport("env", "ext_storage_set_version_1", argv -> {
                    System.out.println("Message printed in the body of 'ext_storage_set_version_1'");
                    return argv;
                }, Arrays.asList(Type.I64, Type.I64), Arrays.asList()),
                new ImportObject.FuncImport("env", "ext_storage_get_version_1", argv -> {
                    System.out.println("Message printed in the body of 'ext_storage_get_version_1'");
                    return argv;
                }, Arrays.asList(Type.I64), Arrays.asList(Type.I64)),
                new ImportObject.FuncImport("env", "ext_misc_print_hex_version_1", argv -> {
                    System.out.println("Message printed in the body of 'ext_misc_print_hex_version_1'");
                    return argv;
                }, Arrays.asList(Type.I64), Arrays.asList()),
                new ImportObject.FuncImport("env", "ext_misc_print_utf8_version_1", argv -> {
                    System.out.println("Message printed in the body of 'ext_misc_print_utf8_version_1'");
                    return argv;
                }, Arrays.asList(Type.I64), Arrays.asList()),
                new ImportObject.FuncImport("env", "ext_misc_runtime_version_version_1", argv -> {
                    System.out.println("Message printed in the body of 'ext_misc_runtime_version_version_1'");
                    return argv;
                }, Arrays.asList(Type.I64), Arrays.asList(Type.I64)),
                new ImportObject.FuncImport("env", "ext_hashing_blake2_128_version_1", argv -> {
                    System.out.println("Message printed in the body of 'ext_hashing_blake2_128_version_1'");
                    return argv;
                }, Arrays.asList(Type.I64), Arrays.asList(Type.I32)),
                new ImportObject.FuncImport("env", "ext_hashing_blake2_256_version_1", argv -> {
                    System.out.println("Message printed in the body of 'ext_hashing_blake2_256_version_1'");
                    return argv;
                }, Arrays.asList(Type.I64), Arrays.asList(Type.I32)),
                new ImportObject.FuncImport("env", "ext_hashing_keccak_256_version_1", argv -> {
                    System.out.println("Message printed in the body of 'ext_hashing_keccak_256_version_1'");
                    return argv;
                }, Arrays.asList(Type.I64), Arrays.asList(Type.I32)),
                new ImportObject.FuncImport("env", "ext_hashing_twox_128_version_1", argv -> {
                    System.out.println("Message printed in the body of 'ext_hashing_twox_128_version_1'");
                    return argv;
                }, Arrays.asList(Type.I64), Arrays.asList(Type.I32)),
                new ImportObject.FuncImport("env", "ext_hashing_twox_64_version_1", argv -> {
                    System.out.println("Message printed in the body of 'ext_hashing_twox_64_version_1'");
                    return argv;
                }, Arrays.asList(Type.I64), Arrays.asList(Type.I32)),
                new ImportObject.FuncImport("env", "ext_crypto_ed25519_generate_version_1", argv -> {
                    System.out.println("Message printed in the body of 'ext_crypto_ed25519_generate_version_1'");
                    return argv;
                }, Arrays.asList(Type.I32, Type.I64), Arrays.asList(Type.I32)),
                new ImportObject.FuncImport("env", "ext_crypto_ed25519_verify_version_1", argv -> {
                    System.out.println("Message printed in the body of 'ext_crypto_ed25519_verify_version_1'");
                    return argv;
                }, Arrays.asList(Type.I32, Type.I64, Type.I32), Arrays.asList(Type.I32)),
                new ImportObject.FuncImport("env",
                        "ext_crypto_secp256k1_ecdsa_recover_version_2", argv -> {
                    System.out.println("Message printed in the body of 'ext_crypto_secp256k1_ecdsa_recover_version_2'");
                    return argv;
                }, Arrays.asList(Type.I32, Type.I32), Arrays.asList(Type.I64)),
                new ImportObject.FuncImport("env",
                        "ext_crypto_secp256k1_ecdsa_recover_compressed_version_2", argv -> {
                    System.out.println("Message printed in the body of " +
                            "'ext_crypto_secp256k1_ecdsa_recover_compressed_version_2'");
                    return argv;
                }, Arrays.asList(Type.I32, Type.I32), Arrays.asList(Type.I64)),
                new ImportObject.FuncImport("env", "ext_crypto_sr25519_generate_version_1", argv -> {
                    System.out.println("Message printed in the body of 'ext_crypto_sr25519_generate_version_1'");
                    return argv;
                }, Arrays.asList(Type.I32, Type.I64), Arrays.asList(Type.I32)),
                new ImportObject.FuncImport("env",
                        "ext_crypto_sr25519_public_keys_version_1", argv -> {
                    System.out.println("Message printed in the body of 'ext_crypto_sr25519_public_keys_version_1'");
                    return argv;
                }, Arrays.asList(Type.I32), Arrays.asList(Type.I64)),
                new ImportObject.FuncImport("env", "ext_crypto_sr25519_sign_version_1", argv -> {
                    System.out.println("Message printed in the body of 'ext_crypto_sr25519_sign_version_1'");
                    return argv;
                }, Arrays.asList(Type.I32, Type.I32, Type.I64), Arrays.asList(Type.I64)),
                new ImportObject.FuncImport("env", "ext_crypto_sr25519_verify_version_2", argv -> {
                    System.out.println("Message printed in the body of 'ext_crypto_sr25519_verify_version_2'");
                    return argv;
                }, Arrays.asList(Type.I32, Type.I64, Type.I32), Arrays.asList(Type.I32)),
                new ImportObject.FuncImport("env", "ext_storage_append_version_1", argv -> {
                    System.out.println("Message printed in the body of 'ext_storage_append_version_1'");
                    return argv;
                }, Arrays.asList(Type.I64, Type.I64), Arrays.asList()),
                new ImportObject.FuncImport("env", "ext_storage_clear_version_1", argv -> {
                    System.out.println("Message printed in the body of 'ext_storage_clear_version_1'");
                    return argv;
                }, Arrays.asList(Type.I64), Arrays.asList()),
                new ImportObject.FuncImport("env", "ext_storage_clear_prefix_version_2", argv -> {
                    System.out.println("Message printed in the body of 'ext_storage_clear_prefix_version_2'");
                    return argv;
                }, Arrays.asList(Type.I64, Type.I64), Arrays.asList(Type.I64)),
                new ImportObject.FuncImport("env", "ext_storage_commit_transaction_version_1", argv -> {
                    System.out.println("Message printed in the body of 'ext_storage_commit_transaction_version_1'");
                    return argv;
                }, Arrays.asList(), Arrays.asList()),
                new ImportObject.FuncImport("env", "ext_storage_exists_version_1", argv -> {
                    System.out.println("Message printed in the body of 'ext_storage_exists_version_1'");
                    return argv;
                }, Arrays.asList(Type.I64), Arrays.asList(Type.I32)),
                new ImportObject.FuncImport("env", "ext_storage_next_key_version_1", argv -> {
                    System.out.println("Message printed in the body of 'ext_storage_next_key_version_1'");
                    return argv;
                }, Arrays.asList(Type.I64), Arrays.asList(Type.I64)),
                new ImportObject.FuncImport("env", "ext_storage_read_version_1", argv -> {
                    System.out.println("Message printed in the body of 'ext_storage_read_version_1'");
                    return argv;
                }, Arrays.asList(Type.I64, Type.I64, Type.I32), Arrays.asList(Type.I64)),
                new ImportObject.FuncImport("env",
                        "ext_storage_rollback_transaction_version_1", argv -> {
                    System.out.println("Message printed in the body of 'ext_storage_rollback_transaction_version_1'");
                    return argv;
                }, Arrays.asList(), Arrays.asList()),
                new ImportObject.FuncImport("env", "ext_storage_root_version_2", argv -> {
                    System.out.println("Message printed in the body of 'ext_storage_root_version_2'");
                    return argv;
                }, Arrays.asList(Type.I32), Arrays.asList(Type.I64)),
                new ImportObject.FuncImport("env",
                        "ext_storage_start_transaction_version_1", argv -> {
                    System.out.println("Message printed in the body of 'ext_storage_start_transaction_version_1'");
                    return argv;
                }, Arrays.asList(), Arrays.asList()),
                new ImportObject.FuncImport("env",
                        "ext_trie_blake2_256_ordered_root_version_2", argv -> {
                    System.out.println("Message printed in the body of 'ext_trie_blake2_256_ordered_root_version_2'");
                    return argv;
                }, Arrays.asList(Type.I64, Type.I32), Arrays.asList(Type.I32)),
                new ImportObject.FuncImport("env", "ext_offchain_is_validator_version_1", argv -> {
                    System.out.println("Message printed in the body of 'ext_offchain_is_validator_version_1'");
                    return argv;
                }, Arrays.asList(), Arrays.asList(Type.I32)),
                new ImportObject.FuncImport("env",
                        "ext_offchain_local_storage_clear_version_1", argv -> {
                    System.out.println("Message printed in the body of 'ext_offchain_local_storage_clear_version_1'");
                    return argv;
                }, Arrays.asList(Type.I32, Type.I64), Arrays.asList()),
                new ImportObject.FuncImport("env",
                        "ext_offchain_local_storage_compare_and_set_version_1", argv -> {
                    System.out.println("Message printed in the body of " +
                            "'ext_offchain_local_storage_compare_and_set_version_1'");
                    return argv;
                }, Arrays.asList(Type.I32, Type.I64, Type.I64, Type.I64), Arrays.asList(Type.I32)),
                new ImportObject.FuncImport("env",
                        "ext_offchain_local_storage_get_version_1", argv -> {
                    System.out.println("Message printed in the body of 'ext_offchain_local_storage_get_version_1'");
                    return argv;
                }, Arrays.asList(Type.I32, Type.I64), Arrays.asList(Type.I64)),
                new ImportObject.FuncImport("env",
                        "ext_offchain_local_storage_set_version_1", argv -> {
                    System.out.println("Message printed in the body of 'ext_offchain_local_storage_set_version_1'");
                    return argv;
                }, Arrays.asList(Type.I32, Type.I64, Type.I64), Arrays.asList()),
                new ImportObject.FuncImport("env", "ext_offchain_network_state_version_1", argv -> {
                    System.out.println("Message printed in the body of 'ext_offchain_network_state_version_1'");
                    return argv;
                }, Arrays.asList(), Arrays.asList(Type.I64)),
                new ImportObject.FuncImport("env", "ext_offchain_random_seed_version_1", argv -> {
                    System.out.println("Message printed in the body of 'ext_offchain_random_seed_version_1'");
                    return argv;
                }, Arrays.asList(), Arrays.asList(Type.I32)),
                new ImportObject.FuncImport("env",
                        "ext_offchain_submit_transaction_version_1", argv -> {
                    System.out.println("Message printed in the body of 'ext_offchain_submit_transaction_version_1'");
                    return argv;
                }, Arrays.asList(Type.I64), Arrays.asList(Type.I64)),
                new ImportObject.FuncImport("env", "ext_offchain_timestamp_version_1", argv -> {
                    System.out.println("Message printed in the body of 'ext_offchain_timestamp_version_1'");
                    return argv;
                }, Arrays.asList(), Arrays.asList(Type.I64)),
                new ImportObject.FuncImport("env", "ext_allocator_free_version_1", argv -> {
                    System.out.println("Message printed in the body of 'ext_allocator_free_version_1'");
                    return argv;
                }, Arrays.asList(Type.I32), Arrays.asList()),
                new ImportObject.FuncImport("env", "ext_allocator_malloc_version_1", argv -> {
                    System.out.println("Message printed in the body of 'ext_allocator_malloc_version_1'");
                    return argv;
                }, Arrays.asList(Type.I32), Arrays.asList(Type.I32)),
                new ImportObject.FuncImport("env", "ext_offchain_index_set_version_1", argv -> {
                    System.out.println("Message printed in the body of 'ext_offchain_index_set_version_1'");
                    return argv;
                }, Arrays.asList(Type.I64, Type.I64), Arrays.asList()),
                new ImportObject.FuncImport("env",
                        "ext_default_child_storage_clear_version_1", argv -> {
                    System.out.println("Message printed in the body of 'ext_default_child_storage_clear_version_1'");
                    return argv;
                }, Arrays.asList(Type.I64, Type.I64), Arrays.asList()),
                new ImportObject.FuncImport("env", "ext_misc_print_hex_version_1", argv -> {
                    System.out.println("Message printed in the body of 'ext_misc_print_hex_version_1'");
                    return argv;
                }, Arrays.asList(Type.I64), Arrays.asList()),
                new ImportObject.FuncImport("env",
                        "ext_default_child_storage_get_version_1", argv -> {
                    System.out.println("Message printed in the body of 'ext_default_child_storage_get_version_1'");
                    return argv;
                }, Arrays.asList(Type.I64, Type.I64), Arrays.asList(Type.I64)),
                new ImportObject.FuncImport("env",
                        "ext_default_child_storage_next_key_version_1", argv -> {
                    System.out.println("Message printed in the body of 'ext_default_child_storage_next_key_version_1'");
                    return argv;
                }, Arrays.asList(Type.I64, Type.I64), Arrays.asList(Type.I64)),
                new ImportObject.FuncImport("env", "ext_default_child_storage_set_version_1", argv -> {
                    System.out.println("Message printed in the body of 'ext_default_child_storage_set_version_1'");
                    return argv;
                }, Arrays.asList(Type.I64, Type.I64, Type.I64), Arrays.asList()),
                new ImportObject.FuncImport("env", "ext_logging_log_version_1", argv -> {
                    System.out.println(argv);
                    System.out.println("Message printed in the body of 'ext_logging_log_version_1'");
                    return argv;
                }, Arrays.asList(Type.I32, Type.I64, Type.I64), Arrays.asList()),
                new ImportObject.FuncImport("env", "ext_misc_print_num_version_1", argv -> {
                    System.out.println(argv);
                    System.out.println("Message printed in the body of 'ext_logging_log_version_1'");
                    return argv;
                }, Arrays.asList(Type.I64), Arrays.asList()),
                memory), module);
    }
}