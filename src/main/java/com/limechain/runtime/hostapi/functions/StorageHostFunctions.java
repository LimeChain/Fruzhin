package com.limechain.runtime.hostapi.functions;

import com.limechain.runtime.hostapi.HostApi;
import lombok.experimental.UtilityClass;
import org.wasmer.ImportObject;
import org.wasmer.Type;

import java.util.Arrays;
import java.util.List;

@UtilityClass
public class StorageHostFunctions {

    public static List<ImportObject> getFunctions(){
        return Arrays.asList(
                HostFunctions.getImportObject( "ext_storage_set_version_1", argv -> {
                    HostApi.extStorageSetVersion1((Long) argv.get(0), (Long) argv.get(1));
                }, List.of(Type.I64, Type.I64)),
                HostFunctions.getImportObject( "ext_storage_get_version_1", argv -> {
                    return List.of(HostApi.extStorageGetVersion1((Long) argv.get(0)));
                }, List.of(Type.I64), Type.I64),
                HostFunctions.getImportObject( "ext_storage_read_version_1", argv -> {
                    return argv;
                }, List.of(Type.I64, Type.I64, Type.I32), Type.I64),
                HostFunctions.getImportObject( "ext_storage_clear_version_1", argv -> {
                    HostApi.extStorageClearVersion1((Long) argv.get(0));
                }, List.of(Type.I64)),
                HostFunctions.getImportObject( "ext_storage_exists_version_1", argv -> {
                    return argv;
                }, List.of(Type.I64), Type.I32),
                HostFunctions.getImportObject( "ext_storage_clear_prefix_version_1", argv -> {
                }, List.of(Type.I64)),
                HostFunctions.getImportObject( "ext_storage_clear_prefix_version_2", argv -> {
                    return List.of(HostApi.extStorageClearPrefixVersion2((Long) argv.get(0),(Long) argv.get(1)));
                }, List.of(Type.I64, Type.I64), Type.I64),
                HostFunctions.getImportObject( "ext_storage_append_version_1", argv -> {
                }, List.of(Type.I64, Type.I64)),
                HostFunctions.getImportObject( "ext_storage_root_version_1", argv -> {
                    return argv;
                }, HostFunctions.EMPTY_LIST_OF_TYPES, Type.I64),
                HostFunctions.getImportObject( "ext_storage_root_version_2", argv -> {
                    return argv;
                }, List.of(Type.I32), Type.I64),
                HostFunctions.getImportObject( "ext_storage_changes_root_version_1", argv -> {
                    return argv;
                }, List.of(Type.I64), Type.I64),
                HostFunctions.getImportObject( "ext_storage_next_key_version_1", argv -> {
                    return argv;
                }, List.of(Type.I64), Type.I64),
                HostFunctions.getImportObject("ext_storage_start_transaction_version_1", argv -> {

                }, HostFunctions.EMPTY_LIST_OF_TYPES),
                HostFunctions.getImportObject("ext_storage_rollback_transaction_version_1", argv -> {

                }, HostFunctions.EMPTY_LIST_OF_TYPES),
                HostFunctions.getImportObject( "ext_storage_commit_transaction_version_1", argv -> {

                }, HostFunctions.EMPTY_LIST_OF_TYPES));
    }
}