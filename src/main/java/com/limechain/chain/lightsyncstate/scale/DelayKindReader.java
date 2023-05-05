package com.limechain.chain.lightsyncstate.scale;

import com.limechain.chain.lightsyncstate.PendingChange;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleReader;
import io.emeraldpay.polkaj.scale.reader.UInt32Reader;

import java.math.BigInteger;

public class DelayKindReader implements ScaleReader<PendingChange.DelayKind> {
    @Override
    public PendingChange.DelayKind read(ScaleCodecReader reader) {
        var enumOrdinal = reader.readUByte();
        switch (enumOrdinal) {
            case 0 -> {
                return new PendingChange.DelayKind() {{
                    this.setKind(PendingChange.DelayKindEnum.Finalized);
                }};
            }
            case 1 -> {
                return new PendingChange.DelayKind() {{
                    this.setKind(PendingChange.DelayKindEnum.Best);
                    this.setMedianLastFinalized(BigInteger.valueOf(new UInt32Reader().read(reader)));
                }};
            }
            default -> throw new IllegalStateException("Unexpected value: " + enumOrdinal);
        }
    }

}