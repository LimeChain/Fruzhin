package com.limechain.runtime.hostapi;

import lombok.Getter;

public enum WasmExports {
    HEAP_BASE("__heap_base"),
    DATA_END("__data_end"),
    MEMORY("memory");

    @Getter
    private final String value;

    WasmExports(String value) {
        this.value = value;
    }
}
