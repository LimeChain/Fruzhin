package com.limechain.network.protocol.transactions.scale;

import io.emeraldpay.polkaj.scale.ScaleCodecWriter;
import io.emeraldpay.polkaj.scale.ScaleWriter;

import java.io.IOException;

public class TransactionsWriter implements ScaleWriter<byte[][]> {

    @Override
    public void write(ScaleCodecWriter writer, byte[][] transactions) throws IOException {
        writer.writeCompact(transactions.length);
        for(int i=0;i<transactions.length;i++){
            writer.writeByteArray(transactions[i]);
        }
    }
}
