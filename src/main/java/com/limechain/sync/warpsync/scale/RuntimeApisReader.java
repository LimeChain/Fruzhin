package com.limechain.sync.warpsync.scale;

import com.limechain.sync.warpsync.runtime.RuntimeApis;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleReader;

import java.math.BigInteger;

import static com.limechain.sync.warpsync.runtime.RuntimeApis.API_VERSION_LENGTH;

public class RuntimeApisReader implements ScaleReader<RuntimeApis> {
    @Override
    public RuntimeApis read(ScaleCodecReader reader) {
        RuntimeApis runtimeApis = new RuntimeApis();
        while (reader.hasNext()) {
            runtimeApis.getApiVersions().add(reader.readByteArray(API_VERSION_LENGTH));
            runtimeApis.getApiVersionsNumbers().add(BigInteger.valueOf(reader.readUint32()));
        }
        return runtimeApis;
    }
}
