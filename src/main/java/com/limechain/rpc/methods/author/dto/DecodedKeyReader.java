package com.limechain.rpc.methods.author.dto;

import com.limechain.storage.crypto.KeyType;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleReader;

import java.util.ArrayList;
import java.util.List;

public class DecodedKeyReader implements ScaleReader<List<DecodedKey>> {
    @Override
    public List<DecodedKey> read(ScaleCodecReader reader) {
        List<DecodedKey> keys = new ArrayList<>();

        reader.skip(2);

        while (reader.hasNext()) {
            reader.skip(1);

            var data = reader.readByteArray(32);
            var keyType = reader.readByteArray(4);

            keys.add(new DecodedKey(data, KeyType.getByBytes(keyType)));
        }

        return keys;
    }
}