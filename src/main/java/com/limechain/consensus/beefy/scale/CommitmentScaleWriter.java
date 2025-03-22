package com.limechain.consensus.beefy.scale;

import com.limechain.consensus.beefy.dto.Commitment;
import com.limechain.consensus.beefy.dto.PayloadElement;
import io.emeraldpay.polkaj.scale.ScaleCodecWriter;
import io.emeraldpay.polkaj.scale.ScaleWriter;
import io.emeraldpay.polkaj.scale.writer.UInt64Writer;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.io.IOException;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CommitmentScaleWriter implements ScaleWriter<Commitment> {

    private static final CommitmentScaleWriter INSTANCE = new CommitmentScaleWriter();

    private final UInt64Writer uint64Writer = new UInt64Writer();

    public static CommitmentScaleWriter getInstance() {
        return INSTANCE;
    }

    @Override
    public void write(ScaleCodecWriter writer, Commitment commitment) throws IOException {

        writer.writeCompact(commitment.getPayload().size());

        for (PayloadElement payloadElement : commitment.getPayload()) {

            writer.writeByteArray(payloadElement.getPayloadId().getId());

            writer.writeCompact(payloadElement.getData().length);
            writer.writeByteArray(payloadElement.getData());
        }

        writer.writeUint32(commitment.getBlockNumber().longValue());
        uint64Writer.write(writer, commitment.getAuthoritySetId());
    }
}
