package com.limechain.sync.warpsync.scale;

import com.limechain.chain.lightsyncstate.Authority;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleReader;
import io.emeraldpay.polkaj.scale.reader.UInt64Reader;
import io.emeraldpay.polkaj.types.Hash256;
import java.math.BigInteger;

public class AuthorityChangesReader implements ScaleReader<Authority[]> {

    @Override
    public Authority[] read(ScaleCodecReader reader) {
        int authoritiesSize = reader.readCompactInt();
        Authority[] authoritiesChanges = new Authority[authoritiesSize];
        for (int i = 0; i < authoritiesSize; i++) {
            Hash256 authority = new Hash256(reader.readByteArray(Hash256.SIZE_BYTES));
            UInt64Reader weightReader = new UInt64Reader();
            BigInteger weight = weightReader.read(reader);
            authoritiesChanges[i] = new Authority(authority, weight);
        }
        return authoritiesChanges;
    }
}
