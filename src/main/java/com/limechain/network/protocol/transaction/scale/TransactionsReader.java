package com.limechain.network.protocol.transaction.scale;

import com.limechain.network.protocol.warp.dto.Extrinsics;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleReader;

public class TransactionsReader implements ScaleReader<Extrinsics[]> {

    @Override
    public Extrinsics[] read(ScaleCodecReader reader) {
        int size = reader.readCompactInt();
        Extrinsics[] transactions = new Extrinsics[size];
        for (int i = 0; i < size; i++) {
            transactions[i] = new Extrinsics(reader.readByteArray());
        }
        return transactions;
    }
}
