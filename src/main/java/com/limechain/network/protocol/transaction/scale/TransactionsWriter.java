package com.limechain.network.protocol.transaction.scale;

import com.limechain.network.protocol.warp.dto.Extrinsics;
import io.emeraldpay.polkaj.scale.ScaleCodecWriter;
import io.emeraldpay.polkaj.scale.ScaleWriter;

import java.io.IOException;

public class TransactionsWriter implements ScaleWriter<Extrinsics[]> {

    @Override
    public void write(ScaleCodecWriter writer, Extrinsics[] transactions) throws IOException {
        writer.writeCompact(transactions.length);
        for (int i = 0; i < transactions.length; i++) {
            writer.writeAsList(transactions[i].getExtrinsic());
        }
    }
}
