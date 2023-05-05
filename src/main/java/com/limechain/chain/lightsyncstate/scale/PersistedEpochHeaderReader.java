package com.limechain.chain.lightsyncstate.scale;

import com.limechain.chain.lightsyncstate.EpochHeader;
import com.limechain.chain.lightsyncstate.PersistedEpochHeader;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleReader;

import java.util.ArrayList;
import java.util.List;

public class PersistedEpochHeaderReader implements ScaleReader<PersistedEpochHeader> {
    @Override
    public PersistedEpochHeader read(ScaleCodecReader reader) {
        PersistedEpochHeader header = new PersistedEpochHeader();
        var enumOrdinal = reader.readUByte();
        List<EpochHeader> headers = new ArrayList<>();
        switch (enumOrdinal) {
            case 0 -> {
                headers.add(reader.read(new EpochHeaderReader()));
                headers.add(reader.read(new EpochHeaderReader()));
            }
            case 1 -> headers.add(reader.read(new EpochHeaderReader()));
            default -> throw new IllegalStateException("Unexpected value: " + enumOrdinal);
        }
        header.setBabeEpochs(headers.toArray(EpochHeader[]::new));
        return header;
    }
}
