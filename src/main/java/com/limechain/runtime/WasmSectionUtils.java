package com.limechain.runtime;

import com.dylibso.chicory.log.SystemLogger;
import com.dylibso.chicory.wasm.Module;
import com.dylibso.chicory.wasm.Parser;
import com.dylibso.chicory.wasm.types.Export;
import com.dylibso.chicory.wasm.types.ExportSection;
import com.dylibso.chicory.wasm.types.Import;
import com.dylibso.chicory.wasm.types.MemoryImport;
import com.dylibso.chicory.wasm.types.MemorySection;
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
import java.util.Optional;

@UtilityClass
public class WasmSectionUtils {
    private static final String RUNTIME_VERSION_SECTION_NAME = "runtime_version";
    private static final String RUNTIME_APIS_SECTION_NAME = "runtime_apis";
    private static final String MEMORY_IMPORT_NAME = "memory";
    private static final String ENV_MODULE_NAME = "env";

    private static final int DEFAULT_MEMORY_PAGES = 2048;

    /**
     * Parses the runtime version if both wasm custom sections ("runtime_apis" and "runtime_version") are present.
     *
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
     *
     * @param wasmBinary the wasm blob
     * @return the parsed {@link org.wasmer.ImportObject.MemoryImport} if present,
     * a default one with 24 initial pages otherwise
     */
    public ImportObject.MemoryImport parseMemoryFromBinary(byte[] wasmBinary) {
        Module moduleWithSections = toModuleWithSections(
            wasmBinary, SectionId.IMPORT, SectionId.MEMORY, SectionId.EXPORT);
        int initialPagesLimit = DEFAULT_MEMORY_PAGES;

        // Per Runtime spec only one memory should be available for each module.
        if (isMemorySectionValid(moduleWithSections.memorySection())
            && isExportSectionValid(moduleWithSections.exportSection())) {
            initialPagesLimit = moduleWithSections.memorySection().getMemory(0).memoryLimits().initialPages();
        } else if (moduleWithSections.importSection() != null) {
            Optional<Import> parsedMemoryImport = moduleWithSections.importSection().stream()
                .filter(i -> i.name().equals(MEMORY_IMPORT_NAME))
                .findFirst();
            if (parsedMemoryImport.isPresent()) {
                initialPagesLimit = ((MemoryImport) parsedMemoryImport.get()).limits().initialPages();
            }
        }

        return new ImportObject.MemoryImport(ENV_MODULE_NAME, initialPagesLimit, false);
    }

    private boolean isMemorySectionValid(@Nullable MemorySection memorySection) {
        return memorySection != null && memorySection.memoryCount() == 1;
    }

    private boolean isExportSectionValid(@Nullable ExportSection exportSection) {
        if (exportSection == null) {
            return false;
        }

        for (int i = 0; i < exportSection.exportCount(); i++) {
            Export export = exportSection.getExport(i);
            if (export.name().equals(MEMORY_IMPORT_NAME)) {
                return true;
            }
        }

        return false;
    }

    private Module toModuleWithSections(byte[] wasmBinary, int... sectionIds) {
        Parser parser = new Parser(new SystemLogger());
        for (int sectionId : sectionIds) {
            parser.includeSection(sectionId);
        }

        return parser.parseModule(new ByteArrayInputStream(wasmBinary));
    }
}
