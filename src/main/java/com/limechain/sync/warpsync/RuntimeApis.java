package com.limechain.sync.warpsync;

import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import lombok.Getter;
import lombok.Setter;

import java.math.BigInteger;
import java.util.ArrayList;

@Getter
@Setter
public class RuntimeApis {
    ArrayList<byte[]> apiVersions = new ArrayList<>();
    ArrayList<BigInteger> apiVersionsNumbers = new ArrayList<>();

    public static RuntimeApis decode(ScaleCodecReader reader) {
        RuntimeApis runtimeApis = new RuntimeApis();
        while (reader.hasNext()) {
            runtimeApis.getApiVersions().add(reader.readByteArray(8));
            runtimeApis.getApiVersionsNumbers().add(BigInteger.valueOf(reader.readUint32()));
        }
        return runtimeApis;
    }

}
