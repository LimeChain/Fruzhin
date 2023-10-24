package com.limechain.runtime.hostapi;

import lombok.experimental.UtilityClass;
import org.wasmer.ImportObject;
import org.wasmer.Type;

import java.util.Arrays;
import java.util.List;

/**
 * Implementations of the Child storage HostAPI functions
 * For more info check
 * {<a href="https://spec.polkadot.network/chap-host-api#sect-child-storage-api">Child Storage API</a>}
 */
@UtilityClass
public class ChildStorageHostFunctions {

    public static List<ImportObject> getFunctions() {
        return Arrays.asList(
                HostApi.getImportObject("ext_default_child_storage_set_version_1", argv -> {
                }, List.of(Type.I64, Type.I64, Type.I64)),
                HostApi.getImportObject(
                        "ext_default_child_storage_get_version_1", argv -> {
                            return 0;
                        }, List.of(Type.I64, Type.I64), Type.I64),
                HostApi.getImportObject("ext_default_child_storage_read_version_1", argv -> {
                    return 0;
                }, List.of(Type.I64, Type.I64, Type.I64, Type.I32), Type.I64),
                HostApi.getImportObject("ext_default_child_storage_clear_version_1", argv -> {
                }, List.of(Type.I64, Type.I64)),
                HostApi.getImportObject("ext_default_child_storage_storage_kill_version_1", argv -> {
                }, List.of(Type.I64)),
                HostApi.getImportObject("ext_default_child_storage_storage_kill_version_2", argv -> {
                    return 0;
                }, List.of(Type.I64, Type.I64), Type.I32),
                HostApi.getImportObject("ext_default_child_storage_storage_kill_version_3", argv -> {
                    return 0;
                }, List.of(Type.I64, Type.I64), Type.I64),
                HostApi.getImportObject("ext_default_child_storage_exists_version_1", argv -> {
                    return 0;
                }, List.of(Type.I64, Type.I64), Type.I32),
                HostApi.getImportObject("ext_default_child_storage_clear_prefix_version_1", argv -> {
                    return 0;
                }, List.of(Type.I64), Type.I64),
                HostApi.getImportObject("ext_default_child_storage_clear_prefix_version_2", argv -> {
                    return 0;
                }, List.of(Type.I64, Type.I64, Type.I64), Type.I64),
                HostApi.getImportObject("ext_default_child_storage_root_version_1", argv -> {
                    return 0;
                }, List.of(Type.I64), Type.I64),
                HostApi.getImportObject("ext_default_child_storage_root_version_2", argv -> {
                    return 0;
                }, List.of(Type.I64, Type.I32), Type.I64),
                HostApi.getImportObject("ext_default_child_storage_next_key_version_1", argv -> {
                    return 0;
                }, List.of(Type.I64, Type.I64), Type.I64));
    }

}
