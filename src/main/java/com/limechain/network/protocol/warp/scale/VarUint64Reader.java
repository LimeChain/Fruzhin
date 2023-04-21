package com.limechain.network.protocol.warp.scale;

import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleReader;

import java.math.BigInteger;

/**
 * Decodes into a `u64` a SCALE-encoded number whose number of bytes isn't known at compile-time.
 * Code translated from:
 * <a href="https://github.com/smol-dot/smoldot/blob/165412f0292009aedd208615a37cf2859fd45936/lib/src/util.rs#L95">
 * nom_varsize_number_decode_u64</a>
 */
public class VarUint64Reader implements ScaleReader<BigInteger> {
    @Override
    public BigInteger read(ScaleCodecReader reader) {
        var out = new byte[8];
        // 4 comes from that blocks are encoded in 4 bytes
        var block = reader.readByteArray(4);
        out[0] = block[0];
        out[1] = block[1];
        out[2] = block[2];
        out[3] = block[3];

        BigInteger result = BigInteger.ZERO;

        result = result.add(BigInteger.valueOf(out[0]));
        result = result.add(BigInteger.valueOf(out[1]).shiftLeft(8));
        result = result.add(BigInteger.valueOf(out[2]).shiftLeft(16));
        result = result.add(BigInteger.valueOf(out[3]).shiftLeft(24));
        result = result.add(BigInteger.valueOf(out[4]).shiftLeft(32));
        result = result.add(BigInteger.valueOf(out[5]).shiftLeft(40));
        result = result.add(BigInteger.valueOf(out[6]).shiftLeft(48));
        result = result.add(BigInteger.valueOf(out[7]).shiftLeft(56));

        return result;

    }
}
