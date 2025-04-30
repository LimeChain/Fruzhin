package com.limechain.utils.scale.writers;

import com.limechain.consensus.babe.dto.InherentData;
import com.limechain.consensus.babe.dto.InherentType;
import io.emeraldpay.polkaj.scale.ScaleCodecWriter;
import io.emeraldpay.polkaj.scale.ScaleWriter;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BlockInherentsWriter implements ScaleWriter<InherentData> {

    private static final BlockInherentsWriter INSTANCE = new BlockInherentsWriter();

    public static BlockInherentsWriter getInstance() {
        return INSTANCE;
    }

    public void write(ScaleCodecWriter writer, InherentData request) throws IOException {

        LinkedHashMap<InherentType, byte[]> data = request.getData();
        writer.writeCompact(data.size());

        for (Map.Entry<InherentType, byte[]> entry : data.entrySet()) {
            byte[] key = entry.getKey().toByteArray();
            byte[] value = entry.getValue();

            writer.writeByteArray(key);
            writer.writeAsList(value);
        }
    }
}
