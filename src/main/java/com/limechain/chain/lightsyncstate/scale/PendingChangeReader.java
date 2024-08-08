package com.limechain.chain.lightsyncstate.scale;

import com.limechain.chain.lightsyncstate.PendingChange;
import com.limechain.polkaj.Hash256;
import com.limechain.polkaj.reader.ListReader;
import com.limechain.polkaj.reader.ScaleCodecReader;
import com.limechain.polkaj.reader.ScaleReader;

import java.math.BigInteger;

public class PendingChangeReader implements ScaleReader<PendingChange> {
    @Override
    public PendingChange read(ScaleCodecReader reader) {
        PendingChange pendingChange = new PendingChange();
        pendingChange.setNextAuthorities(reader.read(new ListReader<>(new AuthorityReader())));
        pendingChange.setDelay(BigInteger.valueOf(reader.readUint32()));
        pendingChange.setCanonHeight(BigInteger.valueOf(reader.readUint32()));
        pendingChange.setCanonHash(new Hash256(reader.readUint256()));
        pendingChange.setDelayKind(new DelayKindReader().read(reader));
        return pendingChange;
    }
}
