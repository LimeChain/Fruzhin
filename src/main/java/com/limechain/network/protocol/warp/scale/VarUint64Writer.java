package com.limechain.network.protocol.warp.scale;

import io.emeraldpay.polkaj.scale.ScaleCodecWriter;
import io.emeraldpay.polkaj.scale.ScaleWriter;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Encodes a `u64` into a SCALE-encoded number whose number of bytes isn't known at compile-time.
 * Code is reverse of {@link VarUint64Reader}:
 */
public class VarUint64Writer implements ScaleWriter<BigInteger> {
    private final int blockNumSize;

    public VarUint64Writer(int blockNumSize) {
        this.blockNumSize = blockNumSize;
    }

    @Override
    public void write(ScaleCodecWriter scaleCodecWriter, BigInteger bigInteger) throws IOException {
        byte[] input = bigInteger.toByteArray();
        byte[] slice = new byte[blockNumSize];
        System.arraycopy(input, 0, slice, 0, Math.min(input.length, blockNumSize));

        ByteBuffer buffer = ByteBuffer.wrap(slice);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        scaleCodecWriter.writeByteArray(slice);
    }
}
