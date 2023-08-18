package com.limechain.network.protocol.grandpa.messages.neighbour;

import com.limechain.network.protocol.grandpa.messages.GrandpaMessageType;
import io.emeraldpay.polkaj.scale.ScaleCodecWriter;
import io.emeraldpay.polkaj.scale.ScaleWriter;
import io.emeraldpay.polkaj.scale.writer.UInt64Writer;

import java.io.IOException;

public class NeighbourMessageScaleWriter implements ScaleWriter<NeighbourMessage> {

    @Override
    public void write(ScaleCodecWriter writer, NeighbourMessage neighbourMessage) throws IOException {
        writer.writeByte(GrandpaMessageType.NEIGHBOUR.getType());
        writer.writeByte(neighbourMessage.getVersion());
        new UInt64Writer().write(writer, neighbourMessage.getRound());
        new UInt64Writer().write(writer, neighbourMessage.getSetId());
        writer.writeUint32(neighbourMessage.getLastFinalizedBlock().longValue());
    }
}
