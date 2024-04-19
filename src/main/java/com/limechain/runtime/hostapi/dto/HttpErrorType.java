package com.limechain.runtime.hostapi.dto;

import com.limechain.exception.scale.ScaleEncodingException;
import io.emeraldpay.polkaj.scale.ScaleCodecWriter;
import io.emeraldpay.polkaj.scaletypes.Result;
import io.emeraldpay.polkaj.scaletypes.ResultWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public enum HttpErrorType {
    DEADLINE_REACHED(0), IO_ERROR(1), INVALID_ID(2);

    private final int value;

    HttpErrorType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public byte[] scaleEncodedResult() {
        try(
                ByteArrayOutputStream buf = new ByteArrayOutputStream();
                ScaleCodecWriter writer = new ScaleCodecWriter(buf)
        ) {
            Result<Object, Integer> result = new Result<>(Result.ResultMode.ERR, null, value);
            new ResultWriter<Object, Integer>()
                    .writeResult(writer, null, ScaleCodecWriter::writeUint32, result);
            return buf.toByteArray();
        } catch (IOException e) {
            throw new ScaleEncodingException(e);
        }
    }
}
