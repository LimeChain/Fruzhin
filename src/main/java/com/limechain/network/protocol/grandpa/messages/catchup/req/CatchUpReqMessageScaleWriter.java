package com.limechain.network.protocol.grandpa.messages.catchup.req;

import com.limechain.network.protocol.grandpa.messages.GrandpaMessageType;
import io.emeraldpay.polkaj.scale.ScaleCodecWriter;
import io.emeraldpay.polkaj.scale.ScaleWriter;
import io.emeraldpay.polkaj.scale.writer.UInt64Writer;

import java.io.IOException;

public class CatchUpReqMessageScaleWriter implements ScaleWriter<CatchUpReqMessage> {
    @Override
    public void write(ScaleCodecWriter writer, CatchUpReqMessage catchUpReqMessage) throws IOException {
        writer.writeByte(GrandpaMessageType.CATCH_UP_REQUEST.getType());
        new UInt64Writer().write(writer, catchUpReqMessage.getRound());
        new UInt64Writer().write(writer, catchUpReqMessage.getSetId());
    }
}
