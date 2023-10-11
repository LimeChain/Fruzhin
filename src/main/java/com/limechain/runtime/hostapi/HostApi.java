package com.limechain.runtime.hostapi;

import com.limechain.runtime.Runtime;
import com.limechain.runtime.allocator.FreeingBumpHeapAllocator;
import com.limechain.storage.KVRepository;
import lombok.extern.java.Log;
import org.wasmer.ImportObject;
import org.wasmer.Memory;
import org.wasmer.Type;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.logging.Level;

/**
 * Holds common methods and services used by the different
 * HostApi functions implementations
 */
@Log
public class HostApi {
    protected static final List<Number> EMPTY_LIST_OF_NUMBER = List.of();
    protected static final List<Type> EMPTY_LIST_OF_TYPES = List.of();

    protected static final String KEY_TO_IGNORE = ":child_storage:default:";

    protected static KVRepository<String, Object> repository;
    protected static Runtime runtime;

    private final FreeingBumpHeapAllocator allocator = new FreeingBumpHeapAllocator(getHeapBase());

    protected static ImportObject getImportObject(final String functionName,
                                                  final UnaryOperator<List<Number>> function,
                                                  final List<Type> args,
                                                  final Type retType) {
        return new ImportObject.FuncImport("env", functionName, argv -> {
            System.out.printf("Message printed in the body of '%s%n'", functionName);
            return function.apply(argv);
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

    public static byte[] getDataFromMemory(long pointer) {
        int ptr = (int) pointer;
        int ptrLength = (int) (pointer >> 32);
        byte[] data = new byte[ptrLength];

        Memory memory = getMemory();
        memory.buffer().get(ptr, data, 0, ptrLength);
        return data;
    }

    public static int putDataToMemory(byte[] data) {
        Memory memory = getMemory();
        ByteBuffer buffer = getByteBuffer(memory);
        int position = buffer.position();
        buffer.put(position, data, 0, data.length);
        buffer.position(position + data.length);

        return position;
    }

    protected static Memory getMemory() {
        return runtime.getInstance().exports.getMemory("memory");
    }

    protected static int getHeapBase() {
       return runtime.getInstance().exports.getGlobal("heap_base").getIntValue();
    }

    public static void setRuntime(Runtime runtime) {
        HostApi.runtime = runtime;
    }

    public static void setRepository(KVRepository<String, Object> repository) {
        HostApi.repository = repository;
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

    public byte[] getDataFromMemory(RuntimePointerSize runtimePointerSize) {
        Memory memory = getMemory();
        ByteBuffer memoryBuffer = memory.buffer();
        byte[] data = new byte[runtimePointerSize.size()];
        memoryBuffer.position(runtimePointerSize.pointer());
        memoryBuffer.get(data);
        return data;
    }

    public RuntimePointerSize addDataToMemory(byte[] data) {
        RuntimePointerSize allocatedPointer = allocate(data.length);
        writeDataToMemory(allocatedPointer, data);
        return allocatedPointer;
    }

    public RuntimePointerSize allocate(int size) {
       return allocator.allocate(size, getMemory());
    }

    public void deallocate(int pointer) {
        allocator.deallocate(pointer, getMemory());
    }

    public void writeDataToMemory(RuntimePointerSize runtimePointerSize, byte[] data) {
        ByteBuffer memoryBuffer = getMemory().buffer();
        memoryBuffer.position(runtimePointerSize.pointer());
        memoryBuffer.put(data, 0, runtimePointerSize.size());
    }
}
