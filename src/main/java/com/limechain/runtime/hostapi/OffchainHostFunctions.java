package com.limechain.runtime.hostapi;

import lombok.experimental.UtilityClass;
import org.wasmer.ImportObject;
import org.wasmer.Type;

import java.util.Arrays;
import java.util.List;

/**
 * Implementations of the Offchain and Offchain index HostAPI functions
 * For more info check
 * {<a href="https://spec.polkadot.network/chap-host-api#sect-offchain-api">Offchain API</a>}
 * {<a href="https://spec.polkadot.network/chap-host-api#sect-offchainindex-api">Offchain index API</a>}
 */
@UtilityClass
public class OffchainHostFunctions {

    public static List<ImportObject> getFunctions() {
        return Arrays.asList(
                HostApi.getImportObject("ext_offchain_is_validator_version_1", argv -> {
                    return argv;
                }, HostApi.EMPTY_LIST_OF_TYPES, Type.I32),
                HostApi.getImportObject("ext_offchain_submit_transaction_version_1", argv -> {
                    return argv;
                }, List.of(Type.I64), Type.I64),
                HostApi.getImportObject("ext_offchain_network_state_version_1", argv -> {
                    return argv;
                }, HostApi.EMPTY_LIST_OF_TYPES, Type.I64),
                HostApi.getImportObject("ext_offchain_timestamp_version_1", argv -> {
                    return argv;
                }, HostApi.EMPTY_LIST_OF_TYPES, Type.I64),
                HostApi.getImportObject("ext_offchain_sleep_until_version_1", argv -> {

                }, List.of(Type.I64)),
                HostApi.getImportObject("ext_offchain_random_seed_version_1", argv -> {
                    return argv;
                }, HostApi.EMPTY_LIST_OF_TYPES, Type.I32),
                HostApi.getImportObject("ext_offchain_local_storage_set_version_1", argv -> {

                }, List.of(Type.I32, Type.I64, Type.I64)),
                HostApi.getImportObject("ext_offchain_local_storage_clear_version_1", argv -> {

                }, List.of(Type.I32, Type.I64)),
                HostApi.getImportObject("ext_offchain_local_storage_compare_and_set_version_1", argv -> {
                    return argv;
                }, List.of(Type.I32, Type.I64, Type.I64, Type.I64), Type.I32),
                HostApi.getImportObject("ext_offchain_local_storage_get_version_1", argv -> {
                    return argv;
                }, List.of(Type.I32, Type.I64), Type.I64),
                HostApi.getImportObject("ext_offchain_http_request_start_version_1", argv -> {
                    return argv;
                }, List.of(Type.I64, Type.I64, Type.I64), Type.I64),
                HostApi.getImportObject("ext_offchain_http_request_add_header_version_1", argv -> {
                    return argv;
                }, List.of(Type.I32, Type.I64, Type.I64), Type.I64),
                HostApi.getImportObject("ext_offchain_http_request_write_body_version_1", argv -> {
                    return argv;
                }, List.of(Type.I32, Type.I64, Type.I64), Type.I64),
                HostApi.getImportObject("ext_offchain_http_response_wait_version_1", argv -> {
                    return argv;
                }, List.of(Type.I64, Type.I64), Type.I64),
                HostApi.getImportObject("ext_offchain_http_response_headers_version_1", argv -> {
                    return argv;
                }, List.of(Type.I32), Type.I64),
                HostApi.getImportObject("ext_offchain_http_response_read_body_version_1", argv -> {
                    return argv;
                }, List.of(Type.I32, Type.I64, Type.I64), Type.I64),
                HostApi.getImportObject("ext_offchain_index_set_version_1", argv -> {

                }, List.of(Type.I64, Type.I64)),
                HostApi.getImportObject("ext_offchain_index_clear_version_1", argv -> {

                }, List.of(Type.I64))
        );
    }

}
