package com.limechain.runtime;

import org.junit.jupiter.api.Test;
import org.wasmer.Module;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RuntimeWasmTest {
    private static final Path HELLO_WORLD_PATH = Paths.get("src","test","resources","hello_world.wasm");

    // values extracted from a .wat file generated from hello_world.wasm,
    // using https://webassembly.github.io/wabt/demo/wasm2wat/
    private static final int HELLO_WORLD_HEAP_BASE = 1048592;
    private static final int HELLO_WORLD_DATA_END = 1048590;

    @Test
    void runtimeGetHeapBase() throws IOException {
        Module module = new Module(Files.readAllBytes(HELLO_WORLD_PATH));
        Runtime runtime = new Runtime(module, 0);

        int heapBase = runtime.getHeapBase();

        assertEquals(HELLO_WORLD_HEAP_BASE, heapBase);
    }

    @Test
    void runtimeGetDataEnd() throws IOException {
        Module module = new Module(Files.readAllBytes(HELLO_WORLD_PATH));
        Runtime runtime = new Runtime(module, 0);

        int dataEnd = runtime.getDataEnd();

        assertEquals(HELLO_WORLD_DATA_END, dataEnd);
    }
}