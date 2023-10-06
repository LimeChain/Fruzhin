package com.limechain.runtime.hostapi.scale;

import io.emeraldpay.polkaj.scale.ScaleCodecWriter;
import io.emeraldpay.polkaj.scale.ScaleWriter;

import java.io.IOException;

public class ResultWriter implements ScaleWriter<byte[]> {
    @Override
    public void write(ScaleCodecWriter scaleCodecWriter, byte[] bytes) throws IOException {
        scaleCodecWriter.writeByteArray(bytes);
    }

    public void writeResult(ScaleCodecWriter scaleCodecWriter, boolean success) throws IOException {
        if (success){
            scaleCodecWriter.writeByte((byte) 0);
        }else{
            scaleCodecWriter.writeByte((byte) 1);
        }
    }
}
