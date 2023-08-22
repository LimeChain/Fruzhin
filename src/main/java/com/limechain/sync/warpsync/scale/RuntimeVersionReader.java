package com.limechain.sync.warpsync.scale;

import com.limechain.runtime.RuntimeApis;
import com.limechain.runtime.RuntimeVersion;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleReader;

import java.math.BigInteger;

import static com.limechain.runtime.RuntimeApis.API_VERSION_LENGTH;

public class RuntimeVersionReader implements ScaleReader<RuntimeVersion> {
    @Override
    public RuntimeVersion read(ScaleCodecReader reader) {
        RuntimeVersion runtimeVersion = new RuntimeVersion();
        runtimeVersion.setSpecName(reader.readString());
        runtimeVersion.setImplementation(reader.readString());
        runtimeVersion.setAuthoringVersion(BigInteger.valueOf(reader.readUint32()));
        runtimeVersion.setSpecVersion(BigInteger.valueOf(reader.readUint32()));
        runtimeVersion.setImplementationVersion(BigInteger.valueOf(reader.readUint32()));

        // Reads 0 when reading from wasm sections because runtime apis were moved in a different wasm section
        // Reads the actual runtimeApis when decoding the Core_Version call
        RuntimeApis runtimeApis = new RuntimeApis();
        int apiVersionsSize = reader.readCompactInt();
        for (int i = 0; i < apiVersionsSize; i++) {
            runtimeApis.getApiVersions().add(reader.readByteArray(API_VERSION_LENGTH));
            runtimeApis.getApiVersionsNumbers().add(BigInteger.valueOf(reader.readUint32()));
        }

        if (apiVersionsSize > 0) {
            runtimeVersion.setRuntimeApis(runtimeApis);
        }

        runtimeVersion.setTransactionVersion(BigInteger.valueOf(reader.readUint32()));
        runtimeVersion.setStateVersion(BigInteger.valueOf(reader.readUByte()));
        return runtimeVersion;
    }
}
