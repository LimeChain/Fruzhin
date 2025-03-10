package com.limechain.chain.lightsyncstate.scale;

import com.limechain.chain.lightsyncstate.Authority;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleReader;
import io.emeraldpay.polkaj.scale.reader.UInt64Reader;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.math.BigInteger;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AuthorityReader implements ScaleReader<Authority> {

    private static final AuthorityReader INSTANCE = new AuthorityReader();

    public static AuthorityReader getInstance() {
        return INSTANCE;
    }

    @Override
    public Authority read(ScaleCodecReader reader) {
        return read(reader, false, null);
    }

    public Authority readFixedWeight(ScaleCodecReader reader, int fixedWeight) {
        return read(reader, true, fixedWeight);
    }

    private Authority read(ScaleCodecReader reader, boolean isFixedWeight, Integer fixedWeight) {
        return new Authority(reader.readUint256(), isFixedWeight ? BigInteger.valueOf(fixedWeight) : new UInt64Reader().read(reader));
    }
}
