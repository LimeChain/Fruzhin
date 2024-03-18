package com.limechain.runtime.hostapi;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.java.Log;
import org.wasmer.ImportObject;
import org.wasmer.Type;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Holds common methods and services used by the different
 * HostApi functions implementations
 */
@Log
public class HostApi {
    protected static final List<Number> EMPTY_LIST_OF_NUMBER = List.of();
    protected static final List<Type> EMPTY_LIST_OF_TYPES = List.of();

    protected static ImportObject getImportObject(final String functionName,
                                                  final Function<List<Number>, Number> function,
                                                  final List<Type> args,
                                                  final Type retType) {
        return new ImportObject.FuncImport("env", functionName, argv -> {
            log.fine(String.format("Message printed in the body of '%s'%n", functionName));
            return Collections.singletonList(function.apply(argv));
        }, args, Collections.singletonList(retType));
    }

    protected static ImportObject getImportObject(final String functionName,
                                                  final Consumer<List<Number>> function,
                                                  final List<Type> args) {
        return new ImportObject.FuncImport("env", functionName, argv -> {
            log.fine(String.format("Message printed in the body of '%s'%n", functionName));
            function.accept(argv);
            return EMPTY_LIST_OF_NUMBER;
        }, args, EMPTY_LIST_OF_TYPES);
    }
}
