package com.limechain.chain.lightsyncstate.scale;

import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleReader;
import io.emeraldpay.polkaj.scale.reader.UInt64Reader;
import org.javatuples.Pair;

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
