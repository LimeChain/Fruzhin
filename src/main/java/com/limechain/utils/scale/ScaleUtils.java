package com.limechain.utils.scale;

import com.limechain.exception.scale.ScaleEncodingException;
import com.limechain.polkaj.writer.ScaleCodecWriter;
import com.limechain.polkaj.writer.ScaleWriter;
import lombok.experimental.UtilityClass;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
@UtilityClass
public class ScaleUtils {

    @UtilityClass
    public class Encode {
        /**
         * Encodes an object into SCALE format using the provided ScaleWriter.
         *
         * @param writer The ScaleWriter for encoding the object.
         * @param value  The object to encode.
         * @param <T>    The type of the object to encode.
         * @return The encoded byte array.
         * @throws ScaleEncodingException If an unexpected error occurs during encoding.
         */
        public <T> byte[] encode(ScaleWriter<T> writer, T value) {
            try (ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                 ScaleCodecWriter scaleCodecWriter = new ScaleCodecWriter(buffer)) {
                writer.write(scaleCodecWriter, value);
                return buffer.toByteArray();
            } catch (IOException e) {
                throw new ScaleEncodingException("Unexpected exception while encoding.");
            }
        }
    }
}
