package com.limechain.runtime.research.hybrid.hostapi;

import com.limechain.exception.NotImplementedException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.java.Log;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.wasmer.ImportObject;
import org.wasmer.Type;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

@Log
@Getter
@AllArgsConstructor
public enum Endpoint {
    ext_allocator_free_version_1("ext_allocator_free_version_1", List.of(Type.I32), null),
    ext_allocator_malloc_version_1("ext_allocator_malloc_version_1", List.of(Type.I32), Type.I32),
    ext_storage_set_version_1("ext_storage_set_version_1", List.of(Type.I64, Type.I64), null),
    ext_storage_get_version_1("ext_storage_get_version_1", List.of(Type.I64), Type.I64),
    ext_storage_read_version_1("ext_storage_read_version_1", List.of(Type.I64, Type.I64, Type.I32), Type.I64),
    ext_storage_clear_version_1("ext_storage_clear_version_1", List.of(Type.I64), null),
    ext_storage_exists_version_1("ext_storage_exists_version_1", List.of(Type.I64), Type.I32),
    ext_storage_clear_prefix_version_1("ext_storage_clear_prefix_version_1", List.of(Type.I64), null),
    ext_storage_clear_prefix_version_2("ext_storage_clear_prefix_version_2", List.of(Type.I64, Type.I64), Type.I64),
    ext_storage_append_version_1("ext_storage_append_version_1", List.of(Type.I64, Type.I64), null),
    ext_storage_root_version_1("ext_storage_root_version_1", List.of(), Type.I64),
    ext_storage_root_version_2("ext_storage_root_version_2", List.of(Type.I32), Type.I64),
    ext_storage_changes_root_version_1("ext_storage_changes_root_version_1", List.of(Type.I64), Type.I64),
    ext_storage_next_key_version_1("ext_storage_next_key_version_1", List.of(Type.I64), Type.I64),
    ext_storage_start_transaction_version_1("ext_storage_start_transaction_version_1", List.of(), null),
    ext_storage_rollback_transaction_version_1("ext_storage_rollback_transaction_version_1", List.of(), null),
    ext_storage_commit_transaction_version_1("ext_storage_commit_transaction_version_1", List.of(), null),
    ext_trie_blake2_256_root_version_1("ext_trie_blake2_256_root_version_1", List.of(Type.I64), Type.I32),
    ext_trie_blake2_256_root_version_2("ext_trie_blake2_256_root_version_2", List.of(Type.I64, Type.I32), Type.I32),
    ext_trie_blake2_256_ordered_root_version_1("ext_trie_blake2_256_ordered_root_version_1", List.of(Type.I64), Type.I32),
    ext_trie_blake2_256_ordered_root_version_2("ext_trie_blake2_256_ordered_root_version_2", List.of(Type.I64, Type.I32), Type.I32),
    ext_trie_keccak_256_root_version_1("ext_trie_keccak_256_root_version_1", List.of(Type.I64), Type.I32),
    ext_trie_keccak_256_root_version_2("ext_trie_keccak_256_root_version_2", List.of(Type.I64, Type.I32), Type.I32),
    ext_trie_keccak_256_ordered_root_version_1("ext_trie_keccak_256_ordered_root_version_1", List.of(Type.I64), Type.I32),
    ext_trie_keccak_256_ordered_root_version_2("ext_trie_keccak_256_ordered_root_version_2", List.of(Type.I64, Type.I32), Type.I32),
    ext_trie_blake2_256_verify_proof_version_1("ext_trie_blake2_256_verify_proof_version_1", List.of(Type.I32, Type.I64, Type.I64, Type.I64), Type.I32),
    ext_trie_blake2_256_verify_proof_version_2("ext_trie_blake2_256_verify_proof_version_2", List.of(Type.I32, Type.I64, Type.I64, Type.I64, Type.I32), Type.I32),
    ext_trie_keccak_256_verify_proof_version_1("ext_trie_keccak_256_verify_proof_version_1", List.of(Type.I32, Type.I64, Type.I64, Type.I64), Type.I32),
    ext_trie_keccak_256_verify_proof_version_2("ext_trie_keccak_256_verify_proof_version_2", List.of(Type.I32, Type.I64, Type.I64, Type.I64, Type.I32), Type.I32),
    ext_offchain_is_validator_version_1("ext_offchain_is_validator_version_1", List.of(), Type.I32),
    ext_offchain_submit_transaction_version_1("ext_offchain_submit_transaction_version_1", List.of(Type.I64), Type.I64),
    ext_offchain_network_state_version_1("ext_offchain_network_state_version_1", List.of(), Type.I64),
    ext_offchain_timestamp_version_1("ext_offchain_timestamp_version_1", List.of(), Type.I64),
    ext_offchain_sleep_until_version_1("ext_offchain_sleep_until_version_1", List.of(Type.I64), null),
    ext_offchain_random_seed_version_1("ext_offchain_random_seed_version_1", List.of(), Type.I32),
    ext_offchain_local_storage_set_version_1("ext_offchain_local_storage_set_version_1", List.of(Type.I32, Type.I64, Type.I64), null),
    ext_offchain_local_storage_clear_version_1("ext_offchain_local_storage_clear_version_1", List.of(Type.I32, Type.I64), null),
    ext_offchain_local_storage_compare_and_set_version_1("ext_offchain_local_storage_compare_and_set_version_1", List.of(Type.I32, Type.I64, Type.I64, Type.I64), Type.I32),
    ext_offchain_local_storage_get_version_1("ext_offchain_local_storage_get_version_1", List.of(Type.I32, Type.I64), Type.I64),
    ext_offchain_http_request_start_version_1("ext_offchain_http_request_start_version_1", List.of(Type.I64, Type.I64, Type.I64), Type.I64),
    ext_offchain_http_request_add_header_version_1("ext_offchain_http_request_add_header_version_1", List.of(Type.I32, Type.I64, Type.I64), Type.I64),
    ext_offchain_http_request_write_body_version_1("ext_offchain_http_request_write_body_version_1", List.of(Type.I32, Type.I64, Type.I64), Type.I64),
    ext_offchain_http_response_wait_version_1("ext_offchain_http_response_wait_version_1", List.of(Type.I64, Type.I64), Type.I64),
    ext_offchain_http_response_headers_version_1("ext_offchain_http_response_headers_version_1", List.of(Type.I32), Type.I64),
    ext_offchain_http_response_read_body_version_1("ext_offchain_http_response_read_body_version_1", List.of(Type.I32, Type.I64, Type.I64), Type.I64),
    ext_offchain_index_set_version_1("ext_offchain_index_set_version_1", List.of(Type.I64, Type.I64), null),
    ext_offchain_index_clear_version_1("ext_offchain_index_clear_version_1", List.of(Type.I64), null),
    ext_misc_print_num_version_1("ext_misc_print_num_version_1", List.of(Type.I64), null),
    ext_misc_print_utf8_version_1("ext_misc_print_utf8_version_1", List.of(Type.I64), null),
    ext_misc_print_hex_version_1("ext_misc_print_hex_version_1", List.of(Type.I64), null),
    ext_misc_runtime_version_version_1("ext_misc_runtime_version_version_1", List.of(Type.I64), Type.I64),
    ext_logging_log_version_1("ext_logging_log_version_1", List.of(Type.I32, Type.I64, Type.I64), null),
    ext_logging_max_level_version_1("ext_logging_max_level_version_1", List.of(), Type.I32),
    ext_hashing_keccak_256_version_1("ext_hashing_keccak_256_version_1", List.of(Type.I64), Type.I32),
    ext_hashing_keccak_512_version_1("ext_hashing_keccak_512_version_1", List.of(Type.I64), Type.I32),
    ext_hashing_sha2_256_version_1("ext_hashing_sha2_256_version_1", List.of(Type.I64), Type.I32),
    ext_hashing_blake2_128_version_1("ext_hashing_blake2_128_version_1", List.of(Type.I64), Type.I32),
    ext_hashing_blake2_256_version_1("ext_hashing_blake2_256_version_1", List.of(Type.I64), Type.I32),
    ext_hashing_twox_64_version_1("ext_hashing_twox_64_version_1", List.of(Type.I64), Type.I32),
    ext_hashing_twox_128_version_1("ext_hashing_twox_128_version_1", List.of(Type.I64), Type.I32),
    ext_hashing_twox_256_version_1("ext_hashing_twox_256_version_1", List.of(Type.I64), Type.I32),
    ext_crypto_ed25519_public_keys_version_1("ext_crypto_ed25519_public_keys_version_1", List.of(Type.I32), Type.I64),
    ext_crypto_ed25519_generate_version_1("ext_crypto_ed25519_generate_version_1", List.of(Type.I32, Type.I64), Type.I32),
    ext_crypto_ed25519_sign_version_1("ext_crypto_ed25519_sign_version_1", List.of(Type.I32, Type.I32, Type.I64), Type.I64),
    ext_crypto_ed25519_verify_version_1("ext_crypto_ed25519_verify_version_1", List.of(Type.I32, Type.I64, Type.I32), Type.I32),
    ext_crypto_ed25519_batch_verify_version_1("ext_crypto_ed25519_batch_verify_version_1", List.of(Type.I32, Type.I64, Type.I32), Type.I32),
    ext_crypto_sr25519_public_keys_version_1("ext_crypto_sr25519_public_keys_version_1", List.of(Type.I32), Type.I64),
    ext_crypto_sr25519_generate_version_1("ext_crypto_sr25519_generate_version_1", List.of(Type.I32, Type.I64), Type.I32),
    ext_crypto_sr25519_sign_version_1("ext_crypto_sr25519_sign_version_1", List.of(Type.I32, Type.I32, Type.I64), Type.I64),
    ext_crypto_sr25519_verify_version_1("ext_crypto_sr25519_verify_version_1", List.of(Type.I32, Type.I64, Type.I32), Type.I32),
    ext_crypto_sr25519_verify_version_2("ext_crypto_sr25519_verify_version_2", List.of(Type.I32, Type.I64, Type.I32), Type.I32),
    ext_crypto_sr25519_batch_verify_version_1("ext_crypto_sr25519_batch_verify_version_1", List.of(Type.I32, Type.I64, Type.I32), Type.I32),
    ext_crypto_ecdsa_public_keys_version_1("ext_crypto_ecdsa_public_keys_version_1", List.of(Type.I64), Type.I64),
    ext_crypto_ecdsa_generate_version_1("ext_crypto_ecdsa_generate_version_1", List.of(Type.I32, Type.I64), Type.I32),
    ext_crypto_ecdsa_sign_version_1("ext_crypto_ecdsa_sign_version_1", List.of(Type.I32, Type.I32, Type.I64), Type.I64),
    ext_crypto_ecdsa_sign_prehashed_version_1("ext_crypto_ecdsa_sign_prehashed_version_1", List.of(Type.I32, Type.I32, Type.I64), Type.I64),
    ext_crypto_ecdsa_verify_version_1("ext_crypto_ecdsa_verify_version_1", List.of(Type.I32, Type.I64, Type.I32), Type.I32),
    ext_crypto_ecdsa_verify_version_2("ext_crypto_ecdsa_verify_version_2", List.of(Type.I32, Type.I64, Type.I32), Type.I32),
    ext_crypto_ecdsa_verify_prehashed_version_1("ext_crypto_ecdsa_verify_prehashed_version_1", List.of(Type.I32, Type.I32, Type.I32), Type.I32),
    ext_crypto_ecdsa_batch_verify_version_1("ext_crypto_ecdsa_batch_verify_version_1", List.of(Type.I32, Type.I64, Type.I32), Type.I32),
    ext_crypto_secp256k1_ecdsa_recover_version_1("ext_crypto_secp256k1_ecdsa_recover_version_1", List.of(Type.I32, Type.I32), Type.I64),
    ext_crypto_secp256k1_ecdsa_recover_version_2("ext_crypto_secp256k1_ecdsa_recover_version_2", List.of(Type.I32, Type.I32), Type.I64),
    ext_crypto_secp256k1_ecdsa_recover_compressed_version_1("ext_crypto_secp256k1_ecdsa_recover_compressed_version_1", List.of(Type.I32, Type.I32), Type.I64),
    ext_crypto_secp256k1_ecdsa_recover_compressed_version_2("ext_crypto_secp256k1_ecdsa_recover_compressed_version_2", List.of(Type.I32, Type.I32), Type.I64),
    ext_crypto_start_batch_verify_version_1("ext_crypto_start_batch_verify_version_1", List.of(), null),
    ext_crypto_finish_batch_verify_version_1("ext_crypto_finish_batch_verify_version_1", List.of(), null),
    ext_default_child_storage_set_version_1("ext_default_child_storage_set_version_1", List.of(Type.I64, Type.I64, Type.I64), null),
    ext_default_child_storage_get_version_1("ext_default_child_storage_get_version_1", List.of(Type.I64, Type.I64), Type.I64),
    ext_default_child_storage_read_version_1("ext_default_child_storage_read_version_1", List.of(Type.I64, Type.I64, Type.I64, Type.I32), Type.I64),
    ext_default_child_storage_clear_version_1("ext_default_child_storage_clear_version_1", List.of(Type.I64, Type.I64), null),
    ext_default_child_storage_storage_kill_version_1("ext_default_child_storage_storage_kill_version_1", List.of(Type.I64), null),
    ext_default_child_storage_storage_kill_version_2("ext_default_child_storage_storage_kill_version_2", List.of(Type.I64, Type.I64), Type.I32),
    ext_default_child_storage_storage_kill_version_3("ext_default_child_storage_storage_kill_version_3", List.of(Type.I64, Type.I64), Type.I64),
    ext_default_child_storage_exists_version_1("ext_default_child_storage_exists_version_1", List.of(Type.I64, Type.I64), Type.I32),
    ext_default_child_storage_clear_prefix_version_1("ext_default_child_storage_clear_prefix_version_1", List.of(Type.I64, Type.I64), null),
    ext_default_child_storage_clear_prefix_version_2("ext_default_child_storage_clear_prefix_version_2", List.of(Type.I64, Type.I64, Type.I64), Type.I64),
    ext_default_child_storage_root_version_1("ext_default_child_storage_root_version_1", List.of(Type.I64), Type.I64),
    ext_default_child_storage_root_version_2("ext_default_child_storage_root_version_2", List.of(Type.I64, Type.I32), Type.I64),
    ext_default_child_storage_next_key_version_1("ext_default_child_storage_next_key_version_1", List.of(Type.I64, Type.I64), Type.I64),
    ;

    @NotNull
    private final String functionName;
    @NotNull
    private final List<Type> args;
    @Nullable
    private final Type retType;

    /**
     * An implementation for an endpoint with a non-void return type.
     */
    public ImportObject getImportObject(Function<List<Number>, Number> impl) {
        if (retType == null) {
            throw new RuntimeException(String.format("The Host API endpoint '%s' returns no value, wrong implementation provided.", functionName));
        }

        return new ImportObject.FuncImport("env", functionName, argv -> {
            log.info(String.format("Host API endpoint invoked: '%s'%n", functionName));
            return Collections.singletonList(impl.apply(argv));
        }, args, Collections.singletonList(retType));
    }

    /**
     * An implementation for an endpoint with a void return type.
     */
    public ImportObject getImportObject(Consumer<List<Number>> impl) {
        if (retType != null) {
            throw new RuntimeException(String.format("The Host API endpoint '%s' does return a value, wrong implementation provided.", functionName));
        }

        return new ImportObject.FuncImport("env", functionName, argv -> {
            log.info(String.format("Host API endpoint invoked '%s'%n", functionName));
            impl.accept(argv);
            return List.of();
        }, args, List.of());
    }

    /**
     * An implementation throwing a `NotImplementedException`.
     */
    public ImportObject getImportObjectNotImplemented() {
        return new ImportObject.FuncImport("env", functionName, argv -> {
            throw new NotImplementedException(String.format("The Host API endpoint '%s' is not yet implemented.", functionName));
        }, args, this.retType == null ? List.of() : List.of(this.retType));
    }
}
