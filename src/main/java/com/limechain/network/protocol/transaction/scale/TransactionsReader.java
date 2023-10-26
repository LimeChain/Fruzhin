package com.limechain.network.protocol.transaction.scale;

import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleReader;

public class TransactionsReader implements ScaleReader<byte[][]> {
    @Override
    public byte[][] read(ScaleCodecReader reader) {
        int size = reader.readCompactInt();
        byte[][] transactions = new byte[size][];
        for (int i = 0; i < size; i++) {
            transactions[i] = reader.readByteArray();
        }
        return transactions;
    }
}
