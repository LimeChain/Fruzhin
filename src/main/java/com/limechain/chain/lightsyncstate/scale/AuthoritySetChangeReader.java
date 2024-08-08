package com.limechain.chain.lightsyncstate.scale;

import com.limechain.polkaj.reader.ScaleCodecReader;
import com.limechain.polkaj.reader.ScaleReader;
import com.limechain.polkaj.reader.UInt64Reader;
import com.limechain.tuple.Pair;

import java.math.BigInteger;

public class AuthoritySetChangeReader implements ScaleReader<Pair<BigInteger, BigInteger>> {
    @Override
    public Pair<BigInteger, BigInteger> read(ScaleCodecReader reader) {
        return new Pair<>(
                new UInt64Reader().read(reader),
                BigInteger.valueOf(reader.readUint32())
        );
    }
}
