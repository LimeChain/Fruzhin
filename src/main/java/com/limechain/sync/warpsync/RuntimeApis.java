package com.limechain.sync.warpsync;

import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import lombok.Getter;
import lombok.Setter;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;

@Getter
@Setter
public class RuntimeApis {
    ArrayList<byte[]> apiVersions = new ArrayList<>();
    ArrayList<BigInteger> apiVersionsNumbers = new ArrayList<>();
    public static final int API_VERSION_LENGTH = 8;

    public static RuntimeApis decode(ScaleCodecReader reader) {
        RuntimeApis runtimeApis = new RuntimeApis();
        while (reader.hasNext()) {
            runtimeApis.getApiVersions().add(reader.readByteArray(API_VERSION_LENGTH));
            runtimeApis.getApiVersionsNumbers().add(BigInteger.valueOf(reader.readUint32()));
        }
        return runtimeApis;
    }

    public BigInteger getApiVersion(byte[] key) {
        for (int i = 0; i < apiVersions.size(); i++) {
            if (Arrays.equals(apiVersions.get(i), key)) {
                return apiVersionsNumbers.get(i);
            }
        }
        return BigInteger.valueOf(-1);
    }
}
