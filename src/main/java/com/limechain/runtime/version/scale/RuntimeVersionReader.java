package com.limechain.runtime.version.scale;

import com.limechain.runtime.version.ApiVersion;
import com.limechain.runtime.version.RuntimeVersion;
import com.limechain.runtime.version.StateVersion;
import com.limechain.runtime.version.ApiVersions;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleReader;
import io.emeraldpay.polkaj.scale.reader.ListReader;

import java.math.BigInteger;
import java.util.List;

public class RuntimeVersionReader implements ScaleReader<RuntimeVersion> {
    @Override
    public RuntimeVersion read(ScaleCodecReader reader) {
        RuntimeVersion runtimeVersion = new RuntimeVersion();
        runtimeVersion.setSpecName(reader.readString());
        runtimeVersion.setImplementationName(reader.readString());
        runtimeVersion.setAuthoringVersion(BigInteger.valueOf(reader.readUint32()));
        runtimeVersion.setSpecVersion(BigInteger.valueOf(reader.readUint32()));
        runtimeVersion.setImplementationVersion(BigInteger.valueOf(reader.readUint32()));

        // Read the api versions
        List<ApiVersion> apiVersions = reader.read(new ListReader<>(new ApiVersionReader()));
        runtimeVersion.setApis(ApiVersions.of(apiVersions));

        // Read transaction version if it's present (older runtimes don't include that field)
        BigInteger transactionVersion = reader.hasNext() ? BigInteger.valueOf(reader.readUint32()) : null;
        runtimeVersion.setTransactionVersion(transactionVersion);

        // Read the state version if it's present. Older runtimes miss this field, so StateVersion 0 is to be presumed.
        int stateVersion = reader.hasNext() ? reader.readUByte() : 0;
        runtimeVersion.setStateVersion(StateVersion.fromInt(stateVersion));

        return runtimeVersion;
    }
}
