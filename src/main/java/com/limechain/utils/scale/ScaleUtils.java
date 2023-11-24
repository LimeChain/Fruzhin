package com.limechain.utils.scale;

import com.limechain.utils.scale.exceptions.ScaleDecodingException;
import com.limechain.utils.scale.writers.PairWriter;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleCodecWriter;
import io.emeraldpay.polkaj.scale.ScaleReader;
import io.emeraldpay.polkaj.scale.reader.ListReader;
import io.emeraldpay.polkaj.scale.writer.ListWriter;
import kotlin.Pair;
import lombok.experimental.UtilityClass;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

// TODO:
//  This is currently a helper utility class
//  planned to grow into a unified scale encode/decode util class with whatever methods are useful
//  WIP
// Currently trying out different approaches to spare some of the boilerplate around SCALE en/decoding
@UtilityClass
public class ScaleUtils {

    @UtilityClass
    public class Decode {
        public <T> T decode(byte[] encodedData, ScaleReader<T> reader) {
            try {
                return new ScaleCodecReader(encodedData).read(reader);
            } catch (RuntimeException e) {
                throw new ScaleDecodingException("Error while SCALE decoding.", e);
            }
        }

        public <T> List<T> decodeList(byte[] encodedData, ScaleReader<T> listItemReader) {
            return decode(encodedData, new ListReader<>(listItemReader));
        }
    }

    @UtilityClass
    public class Encode {
        public <K, V> byte[] encode(
            List<Pair<K, V>> pairs,
            Function<K, byte[]> fstSerializer,
            Function<V, byte[]> sndSerializer
        ) throws IOException {
            return encode(
                pairs.stream()
                    .map(p -> new Pair<>(fstSerializer.apply(p.getFirst()), sndSerializer.apply(p.getSecond())))
                    .toList());
        }

        public byte[] encode(List<Pair<byte[], byte[]>> pairs) throws IOException {
            try (ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                 ScaleCodecWriter writer = new ScaleCodecWriter(buffer)) {
                new ListWriter<>(new PairWriter<>(ScaleCodecWriter::writeAsList, ScaleCodecWriter::writeAsList))
                    .write(writer, pairs);
                return buffer.toByteArray();
            }
        }

        public byte[] encode(byte[][] values) throws IOException {
            try (ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                 ScaleCodecWriter writer = new ScaleCodecWriter(buffer)) {
                new ListWriter<>(ScaleCodecWriter::writeAsList)
                    .write(writer, Arrays.asList(values));
                return buffer.toByteArray();
            }
        }
    }
}
