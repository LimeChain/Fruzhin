package com.limechain.network.protocol.warp.scale;

import com.limechain.network.protocol.warp.dto.BlockHeader;
import com.limechain.network.protocol.warp.dto.Precommit;
import com.limechain.network.protocol.warp.dto.WarpSyncJustification;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleReader;
import io.emeraldpay.polkaj.scale.reader.UInt64Reader;
import io.emeraldpay.polkaj.types.Hash256;

public class JustificationReader implements ScaleReader<WarpSyncJustification> {
    @Override
    public WarpSyncJustification read(ScaleCodecReader reader) {
        WarpSyncJustification justification = new WarpSyncJustification();
        justification.setRound(new UInt64Reader().read(reader));
        justification.setTargetHash(new Hash256(reader.readUint256()));
        justification.setTargetBlock(new VarUint64Reader().read(reader));

        int precommitsCount = reader.readCompactInt();
        Precommit[] precommits = new Precommit[precommitsCount];
        for (int i = 0; i < precommitsCount; i++) {
            precommits[i] = new PrecommitReader().read(reader);
        }
        justification.setPrecommits(precommits);

        int ancestryCount = reader.readCompactInt();
        BlockHeader[] ancestries = new BlockHeader[ancestryCount];
        for (int i = 0; i < ancestryCount; i++) {
            ancestries[i] = new BlockHeaderReader().read(reader);
        }
        justification.setAncestryVotes(ancestries);

        return justification;
    }
}
