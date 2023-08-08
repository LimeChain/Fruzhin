package com.limechain.network.protocol.grandpa.messages.neighbour;

import com.limechain.network.protocol.grandpa.messages.GrandpaMessageType;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleReader;
import io.emeraldpay.polkaj.scale.reader.UInt64Reader;

public class NeighbourMessageScaleReader implements ScaleReader<NeighbourMessage> {
    @Override
    public NeighbourMessage read(ScaleCodecReader reader) {
        int messageType = reader.readByte();
        if (messageType != GrandpaMessageType.NEIGHBOUR.getType()) {
            return null;
        }
        NeighbourMessage neighbourMessage = new NeighbourMessage();
        neighbourMessage.setVersion(reader.readByte());
        neighbourMessage.setRound(new UInt64Reader().read(reader));
        neighbourMessage.setSetId(new UInt64Reader().read(reader));
        neighbourMessage.setLastFinalizedBlock(reader.readUint32());
        return neighbourMessage;
    }
}
