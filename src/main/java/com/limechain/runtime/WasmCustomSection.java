package com.limechain.runtime;

import java.util.Arrays;

/**
 * Identifies a wasm custom section
 *
 * @param name - the name of the section (in the wasm sense, as a tag above the byte content)
 * @param content - the byte content of the section
 */
public record WasmCustomSection(byte[] name, byte[] content) {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WasmCustomSection that = (WasmCustomSection) o;
        return Arrays.equals(name, that.name) && Arrays.equals(content, that.content);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(name);
        result = 31 * result + Arrays.hashCode(content);
        return result;
    }
}
