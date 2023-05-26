package com.limechain.sync.warpsync.scale;

import com.limechain.chain.lightsyncstate.Authority;
import com.limechain.sync.warpsync.dto.AuthoritySetChange;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleReader;

import java.math.BigInteger;

public class ForcedChangeReader implements ScaleReader<AuthoritySetChange> {
    @Override
    public AuthoritySetChange read(ScaleCodecReader reader) {
        BigInteger m = BigInteger.valueOf(reader.readUint32());
        Authority[] authoritiesChanges = new AuthorityChangesReader().read(reader);
        BigInteger delay = BigInteger.valueOf(reader.readUint32());
        return new AuthoritySetChange(authoritiesChanges, delay.add(m));
    }
}
