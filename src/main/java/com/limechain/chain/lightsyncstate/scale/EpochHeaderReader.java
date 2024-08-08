package com.limechain.chain.lightsyncstate.scale;

import com.limechain.chain.lightsyncstate.EpochHeader;
import com.limechain.polkaj.reader.ScaleCodecReader;
import com.limechain.polkaj.reader.ScaleReader;
import com.limechain.polkaj.reader.UInt64Reader;

public class EpochHeaderReader implements ScaleReader<EpochHeader> {
    @Override
    public EpochHeader read(ScaleCodecReader reader) {
        EpochHeader header = new EpochHeader();
        header.setStartSlot(new UInt64Reader().read(reader));
        header.setEndSlot(new UInt64Reader().read(reader));
        return header;
    }
}
