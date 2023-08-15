package com.limechain.sync.warpsync.scale;

import com.limechain.runtime.RuntimeVersion;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleReader;

import java.math.BigInteger;

public class RuntimeVersionReader implements ScaleReader<RuntimeVersion> {
    @Override
    public RuntimeVersion read(ScaleCodecReader reader) {
        RuntimeVersion runtimeVersion = new RuntimeVersion();
        runtimeVersion.setSpecName(reader.readString());
        runtimeVersion.setImplementation(reader.readString());
        runtimeVersion.setAuthoringVersion(BigInteger.valueOf(reader.readUint32()));
        runtimeVersion.setSpecVersion(BigInteger.valueOf(reader.readUint32()));
        runtimeVersion.setImplementationVersion(BigInteger.valueOf(reader.readUint32()));

        // Probably only reads a 0 byte since the runtime apis have been moved to a different custom sections
        int apiVersionsSize = reader.readCompactInt();
        byte[][] apiVersions = new byte[apiVersionsSize][];
        BigInteger[] apiVersionsNumbers = new BigInteger[apiVersionsSize];
        for (int i = 0; i < apiVersionsSize; i++) {
            apiVersions[i] = reader.readByteArray(8);
            apiVersionsNumbers[i] = BigInteger.valueOf(reader.readUint32());
        }

        runtimeVersion.setTransactionVersion(BigInteger.valueOf(reader.readUint32()));
        runtimeVersion.setStateVersion(BigInteger.valueOf(reader.readUByte()));
        return runtimeVersion;
    }
}
