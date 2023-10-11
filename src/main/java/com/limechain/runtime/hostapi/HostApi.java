package com.limechain.runtime.hostapi;

import com.limechain.runtime.Runtime;
import com.limechain.runtime.hostapi.dto.RuntimePointerSize;
import com.limechain.storage.KVRepository;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.java.Log;
import org.wasmer.ImportObject;
import org.wasmer.Memory;
import org.wasmer.Type;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;

/**
 * Holds common methods and services used by the different
 * HostApi functions implementations
 */
@Log
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class HostApi {

    private static final HostApi INSTANCE = new HostApi();

    public static HostApi getInstance() {
        return INSTANCE;
    }

    protected static final List<Number> EMPTY_LIST_OF_NUMBER = List.of();
    protected static final List<Type> EMPTY_LIST_OF_TYPES = List.of();

    protected KVRepository<String, Object> repository;
    protected Runtime runtime;

    protected static ImportObject getImportObject(final String functionName,
                                                  final Function<List<Number>, Number> function,
                                                  final List<Type> args,
                                                  final Type retType) {
        return new ImportObject.FuncImport("env", functionName, argv -> {
            System.out.printf("Message printed in the body of '%s%n'", functionName);
            return Collections.singletonList(function.apply(argv));
        }, args, Collections.singletonList(retType));
    }

    protected static ImportObject getImportObject(final String functionName,
                                                  final Consumer<List<Number>> function,
                                                  final List<Type> args) {
        return new ImportObject.FuncImport("env", functionName, argv -> {
            System.out.printf("Message printed in the body of '%s%n'", functionName);
            function.accept(argv);
            return EMPTY_LIST_OF_NUMBER;
        }, args, EMPTY_LIST_OF_TYPES);
    }
    protected Memory getMemory() {
        return runtime.getInstance().exports.getMemory("memory");
    }

    public void setRuntime(Runtime runtime) {
        this.runtime = runtime;
    }

    public void setRepository(KVRepository<String, Object> repository) {
        this.repository = repository;
    }

    protected static ByteBuffer getByteBuffer(Memory memory) {
        ByteBuffer buffer;
        try {
            Field bufferField = memory.getClass().getDeclaredField("buffer");
            bufferField.setAccessible(true);
            buffer = (ByteBuffer) bufferField.get(memory);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            log.log(Level.WARNING, "Unable to get buffer", e);
            buffer = null;
        }
        return buffer != null ? buffer : memory.buffer();
    }

    public byte[] getDataFromMemory(final RuntimePointerSize runtimePointerSize) {
        return getDataFromMemory(runtimePointerSize.pointer(), runtimePointerSize.size());
    }

    public byte[] getDataFromMemory(int pointer, int length) {
        byte[] data = new byte[length];

        Memory memory = getMemory();
        memory.buffer().get(pointer, data, 0, length);
        return data;
    }

    public RuntimePointerSize addDataToMemory(byte[] data) {
        Memory memory = getMemory();
        ByteBuffer buffer = getByteBuffer(memory);
        int position = buffer.position();
        buffer.put(position, data, 0, data.length);
        buffer.position(position + data.length);

        return new RuntimePointerSize(position, data.length);
    }

    public int putDataToMemory(byte[] data) {
        ByteBuffer buffer = getByteBuffer(getMemory());
        int position = buffer.position();
        buffer.put(position, data, 0, data.length);
        buffer.position(position + data.length);

        return position;
    }

    public void putDataToMemoryBuffer(RuntimePointerSize runtimePointerSize, byte[] data) {
        //TODO: implement
    }
}
