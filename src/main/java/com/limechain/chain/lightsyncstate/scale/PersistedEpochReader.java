package com.limechain.chain.lightsyncstate.scale;

import com.limechain.chain.lightsyncstate.BabeEpoch;
import com.limechain.chain.lightsyncstate.PersistedEpoch;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleReader;

import java.util.ArrayList;
import java.util.List;

public class PersistedEpochReader implements ScaleReader<PersistedEpoch> {
    @Override
    public PersistedEpoch read(ScaleCodecReader reader) {
        PersistedEpoch epoch = new PersistedEpoch();
        var enumOrdinal = reader.readUByte();
        List<BabeEpoch> epochs = new ArrayList<>();
        switch (enumOrdinal) {
            case 0 -> {
                epochs.add(reader.read(new BabeEpochReader()));
                epochs.add(reader.read(new BabeEpochReader()));
            }
            case 1 -> epochs.add(reader.read(new BabeEpochReader()));
            default -> throw new IllegalStateException("Unexpected value: " + enumOrdinal);
        }
        epoch.setBabeEpochs(epochs.toArray(BabeEpoch[]::new));
        return epoch;
    }
}
