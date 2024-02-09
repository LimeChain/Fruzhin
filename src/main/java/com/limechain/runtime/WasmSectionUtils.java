package com.limechain.runtime;

import com.limechain.exception.WasmRuntimeException;
import com.limechain.runtime.version.scale.RuntimeVersionReader;
import com.limechain.runtime.version.ApiVersions;
import com.limechain.runtime.version.RuntimeVersion;
import com.limechain.utils.ByteArrayUtils;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Objects;

@UtilityClass
public class WasmSectionUtils {
    private static final byte[] RUNTIME_VERSION_SECTION_NAME = "runtime_version".getBytes();
    private static final byte[] RUNTIME_APIS_SECTION_NAME = "runtime_apis".getBytes();

    /**
     * Parses the runtime version if both wasm custom sections ("runtime_apis" and "runtime_version") are present.
     * @param wasmBinary the wasm blob
     * @return the parsed RuntimeVersion if both custom sections are present, null otherwise
     */
    @Nullable
    public RuntimeVersion parseRuntimeVersionFromCustomSections(byte[] wasmBinary) {
        // byte value of \0asm concatenated with 0x1, 0x0, 0x0, 0x0 from smoldot runtime_version.rs#97
        byte[] searchKey = new byte[]{0x00, 0x61, 0x73, 0x6D, 0x1, 0x0, 0x0, 0x0};

        int searchedKeyIndex = ByteArrayUtils.indexOf(wasmBinary, searchKey);
        if (searchedKeyIndex < 0) {
            throw new WasmRuntimeException("Key not found in runtime code.");
        }

        WasmCustomSection runtimeApis = findRuntimeApisCustomSection(wasmBinary);
        WasmCustomSection runtimeVersion = findRuntimeVersionCustomSection(wasmBinary);

        // If we're missing any of the two custom sections, fallback to `Core_runtime`
        if (Objects.isNull(runtimeApis) || Objects.isNull(runtimeVersion)) {
            return null;
        }

        // Read as many api versions as there are (we don't know the length for some reason).
        ApiVersions apiVersions = ApiVersions.decodeNoLength(runtimeApis.content());

        // Construct the runtime version partially uninitialized,
        // and then immediately set its apis we've read from the custom section.
        RuntimeVersion version = new RuntimeVersionReader().read(new ScaleCodecReader(runtimeVersion.content()));
        version.setApis(apiVersions);

        return version;
    }

    /**
     * Finds the "runtime_apis" custom section in the given wasm binary
     */
    @Nullable
    public WasmCustomSection findRuntimeApisCustomSection(byte[] wasmBinary) {
        return findCustomSection(wasmBinary, RUNTIME_APIS_SECTION_NAME);
    }

    /**
     * Finds the "runtime_version" custom section in the given wasm binary
     */
    @Nullable
    public WasmCustomSection findRuntimeVersionCustomSection(byte[] wasmBinary) {
        return WasmSectionUtils.findCustomSection(wasmBinary, RUNTIME_VERSION_SECTION_NAME);
    }

    /**
     * Finds a wasm custom section by name
     * @param wasmBytes the wasm blob
     * @param sectionName the name of the custom section we're looking for
     * @return the parsed wasm custom section with the given name,
     *         null if no section with this name is found in the wasm blob
     * @see WasmCustomSection for the data conatined in a "found custom section"
     */
    @Nullable
    public WasmCustomSection findCustomSection(byte[] wasmBytes, byte[] sectionName) {
        // Start after the Wasm file header
        int offset = 8;

        while (offset < wasmBytes.length) {
            // Read section ID
            int sectionId = wasmBytes[offset++];

            // Read section size (as varint)
            int sectionSize = 0;
            int shift = 0;
            byte byteRead;
            do {
                byteRead = wasmBytes[offset++];
                sectionSize |= (byteRead & 0x7f) << shift;
                shift += 7;
            } while (byteRead < 0);

            if (sectionId == 0) {
                // Custom section found
                // Extract its name
                byte[] customSectionNameAndContent = new byte[sectionSize];
                System.arraycopy(wasmBytes, offset, customSectionNameAndContent, 0, sectionSize);

                ScaleCodecReader reader = new ScaleCodecReader(customSectionNameAndContent);
                int nameSize = reader.readUByte();
                byte[] sectionNameDecoded = reader.readByteArray(nameSize);

                // If it's the name we're looking for, parse and return
                if (Arrays.equals(sectionNameDecoded, sectionName)) {
                    int contentSize = sectionSize - nameSize - 1;
                    byte[] sectionContent = new byte[contentSize];
                    System.arraycopy(customSectionNameAndContent, nameSize + 1, sectionContent, 0, contentSize);

                    return new WasmCustomSection(sectionNameDecoded, sectionContent);
                }
            }

            // Move the offset to the next section
            offset += sectionSize;
        }

        return null;
    }
}
