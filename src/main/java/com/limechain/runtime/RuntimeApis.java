package com.limechain.runtime;

import lombok.Getter;
import lombok.Setter;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;

@Getter
@Setter
public class RuntimeApis {
    public static final int API_VERSION_LENGTH = 8;
    private ArrayList<byte[]> apiVersions = new ArrayList<>();
    private ArrayList<BigInteger> apiVersionsNumbers = new ArrayList<>();

    public BigInteger getApiVersion(byte[] key) {
        for (int i = 0; i < apiVersions.size(); i++) {
            if (Arrays.equals(apiVersions.get(i), key)) {
                return apiVersionsNumbers.get(i);
            }
        }
        return BigInteger.valueOf(-1);
    }
}
