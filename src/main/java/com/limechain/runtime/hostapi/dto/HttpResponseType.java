package com.limechain.runtime.hostapi.dto;

import com.limechain.utils.scale.exceptions.ScaleEncodingException;
import io.emeraldpay.polkaj.scale.ScaleCodecWriter;
import io.emeraldpay.polkaj.scaletypes.Result;
import io.emeraldpay.polkaj.scaletypes.ResultWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public enum HttpResponseType {
    DEADLINE_REACHED(0), IO_ERROR(1), INVALID_ID(2), FINISHED(3);

    private final int value;

    HttpResponseType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public byte[] scaleEncodedResult() {
        try (
                ByteArrayOutputStream buf = new ByteArrayOutputStream();
                ScaleCodecWriter writer = new ScaleCodecWriter(buf);
        ) {
            if (value == FINISHED.value) {
                Result<Integer, Object>  result = new Result<>(Result.ResultMode.OK, value, null);
                new ResultWriter<Integer, Object>()
                        .writeResult(writer,
                                ScaleCodecWriter::writeUint32,
                                null,
                                result);
            }
            else {
                Result<Object, Integer>  result = new Result<>(Result.ResultMode.ERR, null, value);
                new ResultWriter<Object, Integer>()
                        .writeResult(writer, null, ScaleCodecWriter::writeUint32, result);
            }
            return buf.toByteArray();
        } catch (IOException e) {
            throw new ScaleEncodingException(e);
        }
    }
}
