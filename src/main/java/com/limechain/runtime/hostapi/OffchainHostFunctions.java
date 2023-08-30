package com.limechain.runtime.hostapi;

import lombok.experimental.UtilityClass;
import org.wasmer.ImportObject;
import org.wasmer.Type;

import java.util.Arrays;
import java.util.List;

@UtilityClass
public class OffchainHostFunctions {

    public static List<ImportObject> getFunctions() {
        return Arrays.asList(new ImportObject.FuncImport("env", "ext_offchain_is_validator_version_1", argv -> {
                    System.out.println("Message printed in the body of 'ext_offchain_is_validator_version_1'");
                    return argv;
                }, List.of(), List.of(Type.I32)),
                new ImportObject.FuncImport("env",
                        "ext_offchain_submit_transaction_version_1", argv -> {
                    System.out.println("Message printed in the body of 'ext_offchain_submit_transaction_version_1'");
                    return argv;
                }, List.of(Type.I64), List.of(Type.I64)),
                new ImportObject.FuncImport("env", "ext_offchain_network_state_version_1", argv -> {
                    System.out.println("Message printed in the body of 'ext_offchain_network_state_version_1'");
                    return argv;
                }, List.of(), List.of(Type.I64)),
                new ImportObject.FuncImport("env", "ext_offchain_timestamp_version_1", argv -> {
                    System.out.println("Message printed in the body of 'ext_offchain_timestamp_version_1'");
                    return argv;
                }, List.of(), List.of(Type.I64)),
                new ImportObject.FuncImport("env", "ext_offchain_sleep_until_version_1", argv -> {
                    System.out.println("Message printed in the body of 'ext_offchain_sleep_until_version_1'");
                    return argv;
                }, List.of(Type.I64), List.of()),
                new ImportObject.FuncImport("env", "ext_offchain_random_seed_version_1", argv -> {
                    System.out.println("Message printed in the body of 'ext_offchain_random_seed_version_1'");
                    return argv;
                }, List.of(), List.of(Type.I32)),
                new ImportObject.FuncImport("env",
                        "ext_offchain_local_storage_set_version_1", argv -> {
                    System.out.println("Message printed in the body of 'ext_offchain_local_storage_set_version_1'");
                    return argv;
                }, List.of(Type.I32, Type.I64, Type.I64), List.of()),
                new ImportObject.FuncImport("env",
                        "ext_offchain_local_storage_clear_version_1", argv -> {
                    System.out.println("Message printed in the body of 'ext_offchain_local_storage_clear_version_1'");
                    return argv;
                }, List.of(Type.I32, Type.I64), List.of()),
                new ImportObject.FuncImport("env",
                        "ext_offchain_local_storage_compare_and_set_version_1", argv -> {
                    System.out.println("Message printed in the body of " +
                            "'ext_offchain_local_storage_compare_and_set_version_1'");
                    return argv;
                }, List.of(Type.I32, Type.I64, Type.I64, Type.I64), List.of(Type.I32)),
                new ImportObject.FuncImport("env",
                        "ext_offchain_local_storage_get_version_1", argv -> {
                    System.out.println("Message printed in the body of 'ext_offchain_local_storage_get_version_1'");
                    return argv;
                }, List.of(Type.I32, Type.I64), List.of(Type.I64)),
                new ImportObject.FuncImport("env",
                        "ext_offchain_http_request_start_version_1", argv -> {
                    System.out.println("Message printed in the body of 'ext_offchain_http_request_start_version_1'");
                    return argv;
                }, List.of(Type.I64, Type.I64, Type.I64), List.of(Type.I64)),
                new ImportObject.FuncImport("env",
                        "ext_offchain_http_request_add_header_version_1", argv -> {
                    System.out.println("Message printed in the body of " +
                            "'ext_offchain_http_request_add_header_version_1'");
                    return argv;
                }, List.of(Type.I32, Type.I64, Type.I64), List.of(Type.I64)),
                new ImportObject.FuncImport("env",
                        "ext_offchain_http_request_write_body_version_1", argv -> {
                    System.out.println("Message printed in the body of " +
                            "'ext_offchain_http_request_write_body_version_1'");
                    return argv;
                }, List.of(Type.I32, Type.I64, Type.I64), List.of(Type.I64)),
                new ImportObject.FuncImport("env", "ext_offchain_http_response_wait_version_1", argv -> {
                    System.out.println("Message printed in the body of 'ext_offchain_http_response_wait_version_1'");
                    return argv;
                }, List.of(Type.I64, Type.I64), List.of(Type.I64)),
                new ImportObject.FuncImport("env", "ext_offchain_http_response_headers_version_1", argv -> {
                    System.out.println("Message printed in the body of 'ext_offchain_http_response_headers_version_1'");
                    return argv;
                }, List.of(Type.I32), List.of(Type.I64)),
                new ImportObject.FuncImport("env",
                        "ext_offchain_http_response_read_body_version_1", argv -> {
                    System.out.println("Message printed in the body of " +
                            "'ext_offchain_http_response_read_body_version_1'");
                    return argv;
                }, List.of(Type.I32, Type.I64, Type.I64), List.of(Type.I64)),
                /*
                 * Offchain index
                 */
                new ImportObject.FuncImport("env", "ext_offchain_index_set_version_1", argv -> {
                    System.out.println("Message printed in the body of 'ext_offchain_index_set_version_1'");
                    return argv;
                }, List.of(Type.I64, Type.I64), List.of()),
                new ImportObject.FuncImport("env", "ext_offchain_index_clear_version_1", argv -> {
                    System.out.println("Message printed in the body of 'ext_offchain_index_clear_version_1'");
                    return argv;
                }, List.of(Type.I64), List.of())
        );
    }

}
