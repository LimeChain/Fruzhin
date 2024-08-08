package com.limechain.chain.lightsyncstate.scale;

import com.limechain.chain.lightsyncstate.BabeEpoch;
import com.limechain.polkaj.reader.ScaleCodecReader;
import com.limechain.polkaj.reader.ScaleReader;
import com.limechain.polkaj.reader.UInt64Reader;
import com.limechain.tuple.Pair;

public class BabeConfigReader implements ScaleReader<BabeEpoch.NextBabeConfig> {
    @Override
    public BabeEpoch.NextBabeConfig read(ScaleCodecReader reader) {
        BabeEpoch.NextBabeConfig config = new BabeEpoch.NextBabeConfig();
        config.setC(new Pair<>(new UInt64Reader().read(reader), new UInt64Reader().read(reader)));
        config.setAllowedSlots(BabeEpoch.BabeAllowedSlots.fromId(reader.readUByte()));
        return config;
    }
}
