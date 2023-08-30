package com.limechain.runtime.hostapi;

import lombok.experimental.UtilityClass;
import org.wasmer.ImportObject;
import org.wasmer.Type;

import java.util.Arrays;
import java.util.List;

@UtilityClass
public class StorageHostFunctions {

    public static List<ImportObject> getFunctions(){
        return Arrays.asList(new ImportObject.FuncImport("env", "ext_storage_set_version_1", argv -> {
                    System.out.println("Message printed in the body of 'ext_storage_set_version_1'");
                    HostApi.extStorageSetVersion1((Long) argv.get(0), (Long) argv.get(1));
                    return List.of();
                }, List.of(Type.I64, Type.I64), List.of()),
                new ImportObject.FuncImport("env", "ext_storage_get_version_1", argv -> {
                    System.out.println("Message printed in the body of 'ext_storage_get_version_1'");
                    return List.of(HostApi.extStorageGetVersion1((Long) argv.get(0)));
                }, List.of(Type.I64), List.of(Type.I64)),
                new ImportObject.FuncImport("env", "ext_storage_read_version_1", argv -> {
                    System.out.println("Message printed in the body of 'ext_storage_read_version_1'");
                    return argv;
                }, List.of(Type.I64, Type.I64, Type.I32), List.of(Type.I64)),
                new ImportObject.FuncImport("env", "ext_storage_clear_version_1", argv -> {
                    System.out.println("Message printed in the body of 'ext_storage_clear_version_1'");
                    return argv;
                }, List.of(Type.I64), List.of()),
                new ImportObject.FuncImport("env", "ext_storage_exists_version_1", argv -> {
                    System.out.println("Message printed in the body of 'ext_storage_exists_version_1'");
                    return argv;
                }, List.of(Type.I64), List.of(Type.I32)),
                new ImportObject.FuncImport("env", "ext_storage_clear_prefix_version_1", argv -> {
                    System.out.println("Message printed in the body of 'ext_storage_clear_prefix_version_1'");
                    return argv;
                }, List.of(Type.I64), List.of()),
                new ImportObject.FuncImport("env", "ext_storage_clear_prefix_version_2", argv -> {
                    System.out.println("Message printed in the body of 'ext_storage_clear_prefix_version_2'");
                    return argv;
                }, List.of(Type.I64, Type.I64), List.of(Type.I64)),
                new ImportObject.FuncImport("env", "ext_storage_append_version_1", argv -> {
                    System.out.println("Message printed in the body of 'ext_storage_append_version_1'");
                    return argv;
                }, List.of(Type.I64, Type.I64), List.of()),
                new ImportObject.FuncImport("env", "ext_storage_root_version_1", argv -> {
                    System.out.println("Message printed in the body of 'ext_storage_root_version_1'");
                    return argv;
                }, List.of(), List.of(Type.I64)),
                new ImportObject.FuncImport("env", "ext_storage_root_version_2", argv -> {
                    System.out.println("Message printed in the body of 'ext_storage_root_version_2'");
                    return argv;
                }, List.of(Type.I32), List.of(Type.I64)),
                new ImportObject.FuncImport("env", "ext_storage_changes_root_version_1", argv -> {
                    System.out.println("Message printed in the body of 'ext_storage_changes_root_version_1'");
                    return argv;
                }, List.of(Type.I64), List.of(Type.I64)),
                new ImportObject.FuncImport("env", "ext_storage_next_key_version_1", argv -> {
                    System.out.println("Message printed in the body of 'ext_storage_next_key_version_1'");
                    return argv;
                }, List.of(Type.I64), List.of(Type.I64)),
                new ImportObject.FuncImport("env",
                        "ext_storage_start_transaction_version_1", argv -> {
                    System.out.println("Message printed in the body of 'ext_storage_start_transaction_version_1'");
                    return argv;
                }, List.of(), List.of()),
                new ImportObject.FuncImport("env",
                        "ext_storage_rollback_transaction_version_1", argv -> {
                    System.out.println("Message printed in the body of 'ext_storage_rollback_transaction_version_1'");
                    return argv;
                }, List.of(), List.of()),
                new ImportObject.FuncImport("env", "ext_storage_commit_transaction_version_1", argv -> {
                    System.out.println("Message printed in the body of 'ext_storage_commit_transaction_version_1'");
                    return argv;
                }, List.of(), List.of()));
    }

}
