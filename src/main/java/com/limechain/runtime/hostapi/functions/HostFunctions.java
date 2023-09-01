package com.limechain.runtime.hostapi.functions;

import lombok.experimental.UtilityClass;
import org.wasmer.ImportObject;
import org.wasmer.Type;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

@UtilityClass
public class HostFunctions {

    static final List<Number> EMPTY_LIST_OF_NUMBER = List.of();
    static final List<Type> EMPTY_LIST_OF_TYPES = List.of();

    public static ImportObject getImportObject(final String functionName,
                                               final UnaryOperator<List<Number>> function,
                                               final List<Type> args,
                                               final Type retType) {
        return new ImportObject.FuncImport("env", functionName, argv -> {
            System.out.printf("Message printed in the body of '%s%n'", functionName);
            return function.apply(argv);
        }, args, Arrays.asList(retType));
    }

    public static ImportObject getImportObject(final String functionName,
                                               final Consumer<List<Number>> function,
                                               final List<Type> args) {
        return new ImportObject.FuncImport("env", functionName, argv -> {
            System.out.printf("Message printed in the body of '%s%n'", functionName);
            function.accept(argv);
            return EMPTY_LIST_OF_NUMBER;
        }, args, EMPTY_LIST_OF_TYPES);
    }
}
