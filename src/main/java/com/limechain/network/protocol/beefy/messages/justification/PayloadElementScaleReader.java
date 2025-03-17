package com.limechain.network.protocol.beefy.messages.justification;

import com.limechain.consensus.beefy.dto.BeefyPayloadId;
import com.limechain.consensus.beefy.dto.PayloadElement;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleReader;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.io.ByteArrayOutputStream;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PayloadElementScaleReader implements ScaleReader<PayloadElement> {
    private static final PayloadElementScaleReader INSTANCE = new PayloadElementScaleReader();
    public static final int PAYLOAD_ID_LENGTH = 2;

    public static PayloadElementScaleReader getInstance() {
        return INSTANCE;
    }

    @Override
    public PayloadElement read(ScaleCodecReader reader) {
        BeefyPayloadId payloadId = BeefyPayloadId.fromBytes(reader.readByteArray(PAYLOAD_ID_LENGTH));
        byte[] dataBytes = readRemainingBytes(reader);

        return new PayloadElement(payloadId, dataBytes);
    }

    private byte[] readRemainingBytes(ScaleCodecReader reader) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        while (reader.hasNext()) {
            buffer.write(reader.readByte());
        }
        return buffer.toByteArray();
    }
}
