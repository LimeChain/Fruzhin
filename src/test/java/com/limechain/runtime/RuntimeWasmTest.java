package com.limechain.runtime;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Disabled("This is an integration test.")
class RuntimeWasmTest {
    // TODO:
    //  This wasm is not a valid runtime since it doesn't obey the runtime API (doesn't export `Core_version`).
    //  This test suite needs total refactoring, since our Runtime class models a wasm module obeying the contract
    //  to call it a "Runtime", i.e. the entire runtime API, memory exports, etc. as per spec.
    //  Currently, we don't have a smaller class modelling "an instance of an arbitrary wasm module"
    private static final Path HELLO_WORLD_PATH = Paths.get("src","test","resources","hello_world.wasm");

    // values extracted from a .wat file generated from hello_world.wasm,
    // using https://webassembly.github.io/wabt/demo/wasm2wat/
    private static final int HELLO_WORLD_HEAP_BASE = 1048592;
    private static final int HELLO_WORLD_DATA_END = 1048590;

    @Test
    void runtimeGetHeapBase() throws IOException {
        Runtime runtime =
            new RuntimeBuilder().buildRuntime(Files.readAllBytes(HELLO_WORLD_PATH), RuntimeBuilder.Config.EMPTY);

        int heapBase = runtime.getHeapBase();

        assertEquals(HELLO_WORLD_HEAP_BASE, heapBase);
    }

    @Test
    void runtimeGetDataEnd() throws IOException {
        Runtime runtime =
            new RuntimeBuilder().buildRuntime(Files.readAllBytes(HELLO_WORLD_PATH), RuntimeBuilder.Config.EMPTY);

        int dataEnd = runtime.getDataEnd();

        assertEquals(HELLO_WORLD_DATA_END, dataEnd);
    }
}