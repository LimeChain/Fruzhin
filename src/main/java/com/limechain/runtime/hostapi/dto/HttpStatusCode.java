package com.limechain.runtime.hostapi.dto;

import com.limechain.exception.scale.ScaleEncodingException;
import io.emeraldpay.polkaj.scale.ScaleCodecWriter;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Getter
@AllArgsConstructor
public class HttpStatusCode {
    private static final Integer FINISHED_TYPE = 3;
    private Integer statusCode;
    private HttpErrorType errorType;

    public static HttpStatusCode success(int statusCode) {
        return new HttpStatusCode(statusCode, null);
    }

    public static HttpStatusCode error(HttpErrorType error) {
        return new HttpStatusCode(null, error);
    }
    public boolean hasError() {
        return errorType != null;
    }

    public byte[] scaleEncoded() {
        try (
                ByteArrayOutputStream buf = new ByteArrayOutputStream();
                ScaleCodecWriter writer = new ScaleCodecWriter(buf)
        ) {
            if (hasError()) {
                writer.writeByte(errorType.getValue());
            } else {
                writer.writeByte(FINISHED_TYPE);
                writer.writeUint32(statusCode);
            }
            return buf.toByteArray();
        } catch (IOException e) {
            throw new ScaleEncodingException(e);
        }
    }
}
