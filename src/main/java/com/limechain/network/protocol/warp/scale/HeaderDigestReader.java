package com.limechain.network.protocol.warp.scale;

import com.limechain.network.protocol.warp.dto.ConsensusEngineId;
import com.limechain.network.protocol.warp.dto.DigestType;
import com.limechain.network.protocol.warp.dto.HeaderDigest;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleReader;

public class HeaderDigestReader implements ScaleReader<HeaderDigest> {
    @Override
    public HeaderDigest read(ScaleCodecReader reader) {
        HeaderDigest headerDigest = new HeaderDigest();
        headerDigest.setType(DigestType.fromId(reader.readByte()));
        headerDigest.setId(ConsensusEngineId.fromId(reader.readByteArray(4)));
        switch (headerDigest.type) {
            case CONSENSUS_MESSAGE, PRE_RUNTIME, RUN_ENV_UPDATED -> {
                int messageLength = reader.readCompactInt();
                headerDigest.setMessage(reader.readByteArray(messageLength));
            }
            case SEAL -> headerDigest.setMessage(new byte[0]);
            default -> throw new IllegalArgumentException("Unknown digest type: " + headerDigest.type);
        }
        return headerDigest;
    }
}
