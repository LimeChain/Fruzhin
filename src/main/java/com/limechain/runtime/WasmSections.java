package com.limechain.runtime;

import com.limechain.sync.warpsync.scale.RuntimeApisReader;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import lombok.Getter;
import lombok.extern.java.Log;

import java.util.Arrays;
import java.util.logging.Level;

@Log
@Getter
public class WasmSections {
    public static final byte[] RUNTIME_VERSION_KEY = "runtime_version".getBytes();
    public static final byte[] RUNTIME_APIS_KEY = "runtime_apis".getBytes();
    private RuntimeVersion runtimeVersion = new RuntimeVersion();

    public void parseCustomSections(byte[] wasmBytes) {
        int offset = 8; // Start after the Wasm file header

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
                // Read custom section content
                byte[] customSectionContent = new byte[sectionSize];
                System.arraycopy(wasmBytes, offset, customSectionContent, 0, sectionSize);

                // Process the custom section content as needed
                processCustomSection(customSectionContent);
            }

            // Move the offset to the next section
            offset += sectionSize;
        }
    }

    public void processCustomSection(byte[] customSectionContent) {
        // Process the custom section content
        log.log(Level.INFO, "Custom section found in wasm code "
                + Arrays.copyOfRange(customSectionContent, 0, 100) + "...");
        ScaleCodecReader reader = new ScaleCodecReader(customSectionContent);
        int size = reader.readByte();
        byte[] sectionNameDecoded = reader.readByteArray(size);
        if (Arrays.equals(sectionNameDecoded, RUNTIME_APIS_KEY)) {
            try {
                RuntimeApisReader runtimeApisReader = new RuntimeApisReader();
                RuntimeApis runtimeApis = runtimeApisReader.read(reader);
                runtimeVersion.setRuntimeApis(runtimeApis);
            } catch (Exception e) {
                log.log(Level.INFO, "Failed to decode runtime apis");
            }
        }
        if (Arrays.equals(sectionNameDecoded, RUNTIME_VERSION_KEY)) {
            runtimeVersion.decode(reader);
        }

        //We can add more decoders for the other custom sections here if needed
    }
}
