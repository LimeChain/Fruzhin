package com.limechain.network.protocol.grandpa.scale;

import io.emeraldpay.polkaj.scale.ScaleCodecWriter;
import io.emeraldpay.polkaj.scale.ScaleWriter;
import io.emeraldpay.polkaj.scale.writer.UInt64Writer;

import java.io.IOException;

public class NeighbourMessageScaleWriter implements ScaleWriter<NeighbourMessage> {
    @Override
    public void write(ScaleCodecWriter scaleCodecWriter, NeighbourMessage neighbourMessage) throws IOException {
        UInt64Writer uInt64Writer = new UInt64Writer();
        scaleCodecWriter.writeByte(2);
        scaleCodecWriter.writeByte(neighbourMessage.getVersion());
        uInt64Writer.write(scaleCodecWriter, neighbourMessage.getRound());
        uInt64Writer.write(scaleCodecWriter, neighbourMessage.getSetId());
        scaleCodecWriter.writeUint32(neighbourMessage.getLastFinalizedBlock());
    }
}
