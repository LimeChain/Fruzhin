package com.limechain.sync.fullsync.inherents.scale;

import com.limechain.sync.fullsync.inherents.InherentData;
import com.limechain.utils.scale.writers.PairWriter;
import io.emeraldpay.polkaj.scale.ScaleCodecWriter;
import io.emeraldpay.polkaj.scale.ScaleWriter;
import io.emeraldpay.polkaj.scale.writer.ListWriter;

import java.io.IOException;

public class InherentDataWriter implements ScaleWriter<InherentData> {

    @Override
    public void write(ScaleCodecWriter scaleCodecWriter, InherentData inherentData) throws IOException {
        scaleCodecWriter.write(
            new ListWriter<>(
                new PairWriter<>(
                    ScaleCodecWriter::writeByteArray, // the identifier is always 8 bytes long, so no length included in its encoding
                    ScaleCodecWriter::writeAsList     // the value is opaque byte array, so we need to include the len
                )),
            inherentData.asRawList());
    }
}
