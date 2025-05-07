package com.limechain.consensus.babe.scale.predigest;

import com.limechain.consensus.babe.dto.ParachainInherentData;
import com.limechain.network.protocol.blockannounce.scale.BlockHeaderScaleWriter;
import io.emeraldpay.polkaj.scale.ScaleCodecWriter;
import io.emeraldpay.polkaj.scale.ScaleWriter;
import io.emeraldpay.polkaj.scale.writer.ListWriter;

import java.io.IOException;
import java.util.ArrayList;

public class ParachainInherentDataWriter implements ScaleWriter<ParachainInherentData> {

    private static final ParachainInherentDataWriter INSTANCE = new ParachainInherentDataWriter();

    private ParachainInherentDataWriter() {}

    public static ParachainInherentDataWriter getInstance() {
        return INSTANCE;
    }

    @Override
    public void write(ScaleCodecWriter writer, ParachainInherentData parachainInherentData) throws IOException {
        // currently bitfields, backedCandidates, disputes fields aren't used, so we encode them directly as empty lists
        new ListWriter<>(getInstance()).write(writer, new ArrayList<>());
        new ListWriter<>(getInstance()).write(writer, new ArrayList<>());
        new ListWriter<>(getInstance()).write(writer, new ArrayList<>());
        writer.write(BlockHeaderScaleWriter.getInstance(), parachainInherentData.getParentHeader());
    }
}