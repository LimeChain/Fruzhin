package com.limechain.runtime.hostapi;

import org.wasmer.ImportObject;
import org.wasmer.Type;

import java.util.Arrays;
import java.util.List;

public class ChildStorageHostFunctions {

    public static List<ImportObject> getFunctions() {
        return Arrays.asList(new ImportObject.FuncImport("env", "ext_default_child_storage_set_version_1", argv -> {
                    System.out.println("Message printed in the body of 'ext_default_child_storage_set_version_1'");
                    return argv;
                }, List.of(Type.I64, Type.I64, Type.I64), List.of()),
                new ImportObject.FuncImport("env",
                        "ext_default_child_storage_get_version_1", argv -> {
                    System.out.println("Message printed in the body of 'ext_default_child_storage_get_version_1'");
                    return argv;
                }, List.of(Type.I64, Type.I64), List.of(Type.I64)),
                new ImportObject.FuncImport("env",
                        "ext_default_child_storage_read_version_1", argv -> {
                    System.out.println("Message printed in the body of 'ext_default_child_storage_read_version_1'");
                    return argv;
                }, List.of(Type.I64, Type.I64, Type.I64, Type.I32), List.of(Type.I64)),
                new ImportObject.FuncImport("env",
                        "ext_default_child_storage_clear_version_1", argv -> {
                    System.out.println("Message printed in the body of 'ext_default_child_storage_clear_version_1'");
                    return argv;
                }, List.of(Type.I64, Type.I64), List.of()),
                new ImportObject.FuncImport("env",
                        "ext_default_child_storage_storage_kill_version_1", argv -> {
                    System.out.println("Message printed in the body of " +
                            "'ext_default_child_storage_storage_kill_version_1'");
                    return argv;
                }, List.of(Type.I64), List.of()),
                new ImportObject.FuncImport("env",
                        "ext_default_child_storage_storage_kill_version_2", argv -> {
                    System.out.println("Message printed in the body of " +
                            "'ext_default_child_storage_storage_kill_version_2'");
                    return argv;
                }, List.of(Type.I64, Type.I64), List.of(Type.I32)),
                new ImportObject.FuncImport("env",
                        "ext_default_child_storage_storage_kill_version_3", argv -> {
                    System.out.println("Message printed in the body of " +
                            "'ext_default_child_storage_storage_kill_version_3'");
                    return argv;
                }, List.of(Type.I64, Type.I64), List.of(Type.I64)),
                new ImportObject.FuncImport("env",
                        "ext_default_child_storage_exists_version_1", argv -> {
                    System.out.println("Message printed in the body of 'ext_default_child_storage_exists_version_1'");
                    return argv;
                }, List.of(Type.I64, Type.I64), List.of(Type.I32)),
                new ImportObject.FuncImport("env",
                        "ext_default_child_storage_clear_prefix_version_1", argv -> {
                    System.out.println("Message printed in the body of " +
                            "'ext_default_child_storage_clear_prefix_version_1'");
                    return argv;
                }, List.of(Type.I64), List.of(Type.I64)),
                new ImportObject.FuncImport("env",
                        "ext_default_child_storage_clear_prefix_version_2", argv -> {
                    System.out.println("Message printed in the body of " +
                            "'ext_default_child_storage_clear_prefix_version_2'");
                    return argv;
                }, List.of(Type.I64, Type.I64, Type.I64), List.of(Type.I64)),
                new ImportObject.FuncImport("env",
                        "ext_default_child_storage_root_version_1", argv -> {
                    System.out.println("Message printed in the body of 'ext_default_child_storage_root_version_1'");
                    return argv;
                }, List.of(Type.I64), List.of(Type.I64)),
                new ImportObject.FuncImport("env",
                        "ext_default_child_storage_root_version_2", argv -> {
                    System.out.println("Message printed in the body of 'ext_default_child_storage_root_version_2'");
                    return argv;
                }, List.of(Type.I64, Type.I32), List.of(Type.I64)),
                new ImportObject.FuncImport("env",
                        "ext_default_child_storage_next_key_version_1", argv -> {
                    System.out.println("Message printed in the body of 'ext_default_child_storage_next_key_version_1'");
                    return argv;
                }, List.of(Type.I64, Type.I64), List.of(Type.I64)));
    }

}
