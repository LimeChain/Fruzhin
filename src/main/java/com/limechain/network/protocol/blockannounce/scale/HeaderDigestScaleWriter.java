package com.limechain.network.protocol.blockannounce.scale;

import com.limechain.network.protocol.warp.dto.DigestType;
import com.limechain.network.protocol.warp.dto.HeaderDigest;
import io.emeraldpay.polkaj.scale.ScaleCodecWriter;
import io.emeraldpay.polkaj.scale.ScaleWriter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static com.limechain.network.protocol.warp.dto.DigestType.CONSENSUS_MESSAGE;
import static com.limechain.network.protocol.warp.dto.DigestType.OTHER;
import static com.limechain.network.protocol.warp.dto.DigestType.PRE_RUNTIME;
import static com.limechain.network.protocol.warp.dto.DigestType.SEAL;

public class HeaderDigestScaleWriter implements ScaleWriter<HeaderDigest> {
    private static final List<DigestType> CONTAINING_MESSAGE =
            Arrays.asList(CONSENSUS_MESSAGE, SEAL, PRE_RUNTIME, OTHER);

    @Override
    public void write(ScaleCodecWriter writer, HeaderDigest headerDigest) throws IOException {
        DigestType type = headerDigest.getType();
        writer.writeByte(type.getValue());

        if (CONTAINING_MESSAGE.contains(type)) {
            if(type != OTHER) {
                writer.writeByteArray(headerDigest.getId().getValue());
            }
            writer.writeCompact(headerDigest.getMessage().length);
            writer.writeByteArray(headerDigest.getMessage());
        }
    }
}
