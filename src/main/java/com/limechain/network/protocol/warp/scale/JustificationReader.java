package com.limechain.network.protocol.warp.scale;

import com.limechain.network.protocol.warp.dto.BlockHeader;
import com.limechain.network.protocol.warp.dto.Precommit;
import com.limechain.network.protocol.warp.dto.Justification;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleReader;
import io.emeraldpay.polkaj.scale.reader.UInt64Reader;
import io.emeraldpay.polkaj.types.Hash256;

public class JustificationReader implements ScaleReader<Justification> {
    @Override
    public Justification read(ScaleCodecReader reader) {
        Justification justification = new Justification();
        justification.setRound(new UInt64Reader().read(reader));
        justification.setTargetHash(new Hash256(reader.readUint256()));
        justification.setTargetBlock(new VarUint64Reader(4).read(reader));

        int precommitsCount = reader.readCompactInt();
        Precommit[] precommits = new Precommit[precommitsCount];
        PrecommitReader precommitReader = new PrecommitReader();
        for (int i = 0; i < precommitsCount; i++) {
            precommits[i] = precommitReader.read(reader);
        }
        justification.setPrecommits(precommits);

        int ancestryCount = reader.readCompactInt();
        BlockHeader[] ancestries = new BlockHeader[ancestryCount];
        BlockHeaderReader blockHeaderReader = new BlockHeaderReader();
        for (int i = 0; i < ancestryCount; i++) {
            ancestries[i] = blockHeaderReader.read(reader);
        }
        justification.setAncestryVotes(ancestries);

        return justification;
    }
}
