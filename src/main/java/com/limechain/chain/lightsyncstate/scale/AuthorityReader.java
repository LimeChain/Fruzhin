package com.limechain.chain.lightsyncstate.scale;

import com.limechain.chain.lightsyncstate.Authority;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleReader;
import io.emeraldpay.polkaj.scale.reader.UInt64Reader;

public class AuthorityReader implements ScaleReader<Authority> {
    @Override
    public Authority read(ScaleCodecReader reader) {
        return new Authority(reader.readUint256(), new UInt64Reader().read(reader));
    }
}
