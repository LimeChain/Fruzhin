package com.limechain.chain.lightsyncstate.scale;

import com.limechain.chain.lightsyncstate.Authority;
import com.limechain.polkaj.reader.ScaleCodecReader;
import com.limechain.polkaj.reader.ScaleReader;
import com.limechain.polkaj.reader.UInt64Reader;

public class AuthorityReader implements ScaleReader<Authority> {
    @Override
    public Authority read(ScaleCodecReader reader) {
        return new Authority(reader.readUint256(), new UInt64Reader().read(reader));
    }
}
