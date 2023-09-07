package com.limechain.network.protocol.grandpa.messages.catchupreq;

import com.limechain.network.protocol.grandpa.messages.GrandpaMessageType;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleReader;
import io.emeraldpay.polkaj.scale.reader.UInt64Reader;

public class CatchUpReqMessageScaleReader implements ScaleReader<CatchUpReqMessage> {
    @Override
    public CatchUpReqMessage read(ScaleCodecReader reader) {
        int messageType = reader.readByte();
        if (messageType != GrandpaMessageType.CATCH_UP_REQUEST.getType()) {
            throw new RuntimeException(
                    String.format("Trying to read message of type %d as a catch up request message", messageType));
        }
        CatchUpReqMessage catchUpReqMessage = new CatchUpReqMessage();
        catchUpReqMessage.setRound(new UInt64Reader().read(reader));
        catchUpReqMessage.setSetId(new UInt64Reader().read(reader));

        return catchUpReqMessage;
    }
}
