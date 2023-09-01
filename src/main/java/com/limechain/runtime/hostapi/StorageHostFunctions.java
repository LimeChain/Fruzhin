package com.limechain.runtime.hostapi;

import lombok.experimental.UtilityClass;
import org.wasmer.ImportObject;
import org.wasmer.Type;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

@UtilityClass
public class StorageHostFunctions {

    public static List<ImportObject> getFunctions() {
        return Arrays.asList(
                HostApi.getImportObject("ext_storage_set_version_1", argv -> {
                    extStorageSetVersion1((Long) argv.get(0), (Long) argv.get(1));
                }, List.of(Type.I64, Type.I64)),
                HostApi.getImportObject("ext_storage_get_version_1", argv -> {
                    return List.of(extStorageGetVersion1((Long) argv.get(0)));
                }, List.of(Type.I64), Type.I64),
                HostApi.getImportObject("ext_storage_read_version_1", argv -> {
                    return argv;
                }, List.of(Type.I64, Type.I64, Type.I32), Type.I64),
                HostApi.getImportObject("ext_storage_clear_version_1", argv -> {
                    extStorageClearVersion1((Long) argv.get(0));
                }, List.of(Type.I64)),
                HostApi.getImportObject("ext_storage_exists_version_1", argv -> {
                    return argv;
                }, List.of(Type.I64), Type.I32),
                HostApi.getImportObject("ext_storage_clear_prefix_version_1", argv -> {
                }, List.of(Type.I64)),
                HostApi.getImportObject("ext_storage_clear_prefix_version_2", argv -> {
                    return List.of(extStorageClearPrefixVersion2((Long) argv.get(0), (Long) argv.get(1)));
                }, List.of(Type.I64, Type.I64), Type.I64),
                HostApi.getImportObject("ext_storage_append_version_1", argv -> {
                }, List.of(Type.I64, Type.I64)),
                HostApi.getImportObject("ext_storage_root_version_1", argv -> {
                    return argv;
                }, HostApi.EMPTY_LIST_OF_TYPES, Type.I64),
                HostApi.getImportObject("ext_storage_root_version_2", argv -> {
                    return argv;
                }, List.of(Type.I32), Type.I64),
                HostApi.getImportObject("ext_storage_changes_root_version_1", argv -> {
                    return argv;
                }, List.of(Type.I64), Type.I64),
                HostApi.getImportObject("ext_storage_next_key_version_1", argv -> {
                    return argv;
                }, List.of(Type.I64), Type.I64),
                HostApi.getImportObject("ext_storage_start_transaction_version_1", argv -> {

                }, HostApi.EMPTY_LIST_OF_TYPES),
                HostApi.getImportObject("ext_storage_rollback_transaction_version_1", argv -> {

                }, HostApi.EMPTY_LIST_OF_TYPES),
                HostApi.getImportObject("ext_storage_commit_transaction_version_1", argv -> {

                }, HostApi.EMPTY_LIST_OF_TYPES));
    }

    public static void extStorageSetVersion1(long keyPtr, long valuePtr) {
        byte[] key = HostApi.getDataFromMemory(keyPtr);
        byte[] value = HostApi.getDataFromMemory(valuePtr);

        HostApi.repository.save(new String(key), value);
    }

    public static int extStorageGetVersion1(long keyPtr) {
        byte[] key = HostApi.getDataFromMemory(keyPtr);
        Object data = HostApi.repository.find(new String(key)).orElse(null);
        if (data instanceof byte[] dataArray) {
            return HostApi.putDataToMemory(dataArray);
        }
        return 0;
    }

    public static void extStorageClearVersion1(long keyPtr) {
        byte[] key = HostApi.getDataFromMemory(keyPtr);
        HostApi.repository.delete(new String(key));
    }

    public static long extStorageClearPrefixVersion2(long prefixPtr, long limitPtr) {
        String prefix = new String(HostApi.getDataFromMemory(prefixPtr));
        int limit = new BigInteger(HostApi.getDataFromMemory(limitPtr)).intValue();

        int deleted = HostApi.repository.deletePrefix(prefix, limit);
        //TODO: Count how many are left?

        return 0;
    }
}
