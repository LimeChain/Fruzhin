package com.limechain.network.protocol.grandpa.scale;

import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleReader;
import io.emeraldpay.polkaj.scale.reader.UInt64Reader;

public class NeighbourMessageScaleReader implements ScaleReader<NeighbourMessage> {
    @Override
    public NeighbourMessage read(ScaleCodecReader reader) {
        int messageType = reader.readByte();
        if (messageType != 2) {
            return null;
        }
        UInt64Reader uInt64Reader = new UInt64Reader();
        NeighbourMessage neighbourMessage = new NeighbourMessage();
        neighbourMessage.setVersion(reader.readByte());
        neighbourMessage.setRound(uInt64Reader.read(reader));
        neighbourMessage.setSetId(uInt64Reader.read(reader));
        neighbourMessage.setLastFinalizedBlock(reader.readUint32());
        return neighbourMessage;
    }
}
