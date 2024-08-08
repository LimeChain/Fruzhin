package com.limechain.chain.lightsyncstate.scale;

import com.limechain.chain.lightsyncstate.BabeEpoch;
import com.limechain.polkaj.reader.ListReader;
import com.limechain.polkaj.reader.ScaleCodecReader;
import com.limechain.polkaj.reader.ScaleReader;
import com.limechain.polkaj.reader.UInt64Reader;

public class BabeEpochReader implements ScaleReader<BabeEpoch> {
    @Override
    public BabeEpoch read(ScaleCodecReader reader) {
        BabeEpoch epoch = new BabeEpoch();
        epoch.setEpochIndex(new UInt64Reader().read(reader));
        epoch.setSlotNumber(new UInt64Reader().read(reader));
        epoch.setDuration(new UInt64Reader().read(reader));
        epoch.setAuthorities(reader.read(new ListReader<>(new AuthorityReader())));
        epoch.setRandomness(reader.readUint256());
        epoch.setNextConfig(reader.read(new BabeConfigReader()));
        return epoch;
    }
}
