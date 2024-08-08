package com.limechain.polkaj.reader;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Int32Reader implements ScaleReader<Integer> {
    @Override
    public Integer read(ScaleCodecReader rdr) {
        ByteBuffer buf = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
        buf.put(rdr.readByte());
        buf.put(rdr.readByte());
        buf.put(rdr.readByte());
        buf.put(rdr.readByte());
        return buf.flip().getInt();
    }
}