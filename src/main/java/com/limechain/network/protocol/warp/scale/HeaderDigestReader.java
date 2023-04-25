package com.limechain.network.protocol.warp.scale;

import com.limechain.network.protocol.warp.dto.ConsensusEngine;
import com.limechain.network.protocol.warp.dto.DigestType;
import com.limechain.network.protocol.warp.dto.HeaderDigest;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleReader;

public class HeaderDigestReader implements ScaleReader<HeaderDigest> {
    @Override
    public HeaderDigest read(ScaleCodecReader reader) {
        HeaderDigest headerDigest = new HeaderDigest();
        byte digestType = reader.readByte();
        headerDigest.setType(DigestType.fromId(digestType));
        switch (headerDigest.getType()) {
            case CONSENSUS_MESSAGE, SEAL, PRE_RUNTIME -> {
                headerDigest.setId(ConsensusEngine.fromId(reader.readByteArray(ConsensusEngine.ID_LENGTH)));
                int messageLength = reader.readCompactInt();
                headerDigest.setMessage(reader.readByteArray(messageLength));
            }
            case RUN_ENV_UPDATED -> headerDigest.setMessage(new byte[0]);
            case OTHER -> {
                int messageLength = reader.readCompactInt();
                headerDigest.setMessage(reader.readByteArray(messageLength));
            }
            default -> throw new IllegalArgumentException("Unknown digest type: " + digestType);
        }
        return headerDigest;
    }
}
