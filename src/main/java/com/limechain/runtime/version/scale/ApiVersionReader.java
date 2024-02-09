package com.limechain.runtime.version.scale;

import com.limechain.runtime.version.ApiVersion;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleReader;

import java.math.BigInteger;

public class ApiVersionReader implements ScaleReader<ApiVersion> {

    @Override
    public ApiVersion read(ScaleCodecReader reader) {
        byte[] hashedName = reader.readByteArray(ApiVersion.NAME_HASH_LENGTH);
        long version = reader.readUint32();

        return new ApiVersion(hashedName, BigInteger.valueOf(version));
    }
}
