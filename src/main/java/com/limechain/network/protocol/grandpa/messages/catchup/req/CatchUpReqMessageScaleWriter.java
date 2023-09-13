package com.limechain.network.protocol.grandpa.messages.catchup.req;

import com.limechain.network.protocol.grandpa.messages.GrandpaMessageType;
import io.emeraldpay.polkaj.scale.ScaleCodecWriter;
import io.emeraldpay.polkaj.scale.ScaleWriter;
import io.emeraldpay.polkaj.scale.writer.UInt64Writer;

import java.io.IOException;

public class CatchUpReqMessageScaleWriter implements ScaleWriter<CatchUpReqMessage> {
    private static final CatchUpReqMessageScaleWriter INSTANCE = new CatchUpReqMessageScaleWriter();
    private final UInt64Writer uint64Writer;

    private CatchUpReqMessageScaleWriter() {
        uint64Writer = new UInt64Writer();
    }

    public static CatchUpReqMessageScaleWriter getInstance() {
        return INSTANCE;
    }

    @Override
    public void write(ScaleCodecWriter writer, CatchUpReqMessage catchUpReqMessage) throws IOException {
        writer.writeByte(GrandpaMessageType.CATCH_UP_REQUEST.getType());
        uint64Writer.write(writer, catchUpReqMessage.getRound());
        uint64Writer.write(writer, catchUpReqMessage.getSetId());
    }
}
