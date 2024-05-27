package com.limechain.runtime;

import com.dylibso.chicory.log.SystemLogger;
import com.dylibso.chicory.wasm.Module;
import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.types.ImportSection;
import com.dylibso.chicory.wasm.types.MemoryImport;
import com.dylibso.chicory.wasm.types.SectionId;
import com.dylibso.chicory.wasm.types.UnknownCustomSection;
import com.limechain.runtime.version.ApiVersions;
import com.limechain.runtime.version.RuntimeVersion;
import com.limechain.runtime.version.scale.RuntimeVersionReader;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Nullable;
import org.wasmer.ImportObject;

import java.io.ByteArrayInputStream;
import java.util.Objects;

@UtilityClass
public class WasmSectionUtils {
    private static final String RUNTIME_VERSION_SECTION_NAME = "runtime_version";
    private static final String RUNTIME_APIS_SECTION_NAME = "runtime_apis";

    /**
     * Parses the runtime version if both wasm custom sections ("runtime_apis" and "runtime_version") are present.
     * @param wasmBinary the wasm blob
     * @return the parsed RuntimeVersion if both custom sections are present, null otherwise
     */
    @Nullable
    public RuntimeVersion parseRuntimeVersionFromBinary(byte[] wasmBinary) {
        Module moduleWithCustomSections = toModuleWithSections(wasmBinary, SectionId.CUSTOM);

        UnknownCustomSection runtimeApis = (UnknownCustomSection) moduleWithCustomSections
                .customSection(RUNTIME_APIS_SECTION_NAME);
        UnknownCustomSection runtimeVersion = (UnknownCustomSection) moduleWithCustomSections
                .customSection(RUNTIME_VERSION_SECTION_NAME);

        // If we're missing any of the two custom sections, fallback to `Core_runtime`
        if (Objects.isNull(runtimeApis) || Objects.isNull(runtimeVersion)) {
            return null;
        }

        // Read as many api versions as there are (we don't know the length for some reason).
        ApiVersions apiVersions = ApiVersions.decodeNoLength(runtimeApis.bytes());

        // Construct the runtime version partially uninitialized,
        // and then immediately set its apis we've read from the custom section.
        RuntimeVersion version = new RuntimeVersionReader().read(new ScaleCodecReader(runtimeVersion.bytes()));
        version.setApis(apiVersions);

        return version;
    }

    /**
     * Parses the import section of a wasm blob and extracts the "memory" import if present.
     * @param wasmBinary the wasm blob
     * @return the parsed {@link org.wasmer.ImportObject.MemoryImport} if present,
     * a default one with 24 initial pages otherwise
     */
    public ImportObject.MemoryImport parseMemoryImportFromBinary(byte[] wasmBinary) {
        ImportSection importSection = toModuleWithSections(wasmBinary, SectionId.IMPORT).importSection();
        MemoryImport parsedMemoryImport = (MemoryImport) importSection.stream()
                .filter(i -> i.name().equals("memory"))
                .findFirst()
                .orElse(null);
        return new ImportObject.MemoryImport("env",
                parsedMemoryImport != null ? parsedMemoryImport.limits().initialPages() : 24,
                false);
    }

    private Module toModuleWithSections(byte[] wasmBinary, int... sectionIds) {
        Parser parser = new Parser(new SystemLogger());

        for (int sectionId : sectionIds) {
            parser.includeSection(sectionId);
        }

        return parser.parseModule(new ByteArrayInputStream(wasmBinary));
    }
}
