package com.limechain.chain.lightsyncstate.scale;

import com.limechain.chain.lightsyncstate.Authority;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleReader;
import io.emeraldpay.polkaj.scale.reader.UInt64Reader;
import io.emeraldpay.polkaj.types.Hash256;

public class AuthorityReader implements ScaleReader<Authority> {
    @Override
    public Authority read(ScaleCodecReader reader) {
        Authority authority = new Authority(new Hash256(reader.readUint256()), new UInt64Reader().read(reader));
        return authority;
    }
}
