package com.limechain.utils.scale;

import com.limechain.exception.scale.ScaleDecodingException;
import com.limechain.exception.scale.ScaleEncodingException;
import com.limechain.utils.scale.writers.IterableWriter;
import com.limechain.utils.scale.writers.PairWriter;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleCodecWriter;
import io.emeraldpay.polkaj.scale.ScaleReader;
import io.emeraldpay.polkaj.scale.ScaleWriter;
import io.emeraldpay.polkaj.scale.reader.ListReader;
import io.emeraldpay.polkaj.scale.writer.ListWriter;
import lombok.experimental.UtilityClass;
import org.javatuples.Pair;

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
        public <K, V> byte[] encodeListOfPairs(
            List<Pair<K, V>> pairs,
            Function<K, byte[]> fstSerializer,
            Function<V, byte[]> sndSerializer
        ) {
            return encodeListOfPairs(
                pairs.stream()
                    .map(p ->
                        new Pair<>(
                            fstSerializer.apply(p.getValue0()),
                            sndSerializer.apply(p.getValue1())))
                    .toList());
        }

        public byte[] encodeListOfPairs(List<Pair<byte[], byte[]>> pairs) {
            return encode(
                new ListWriter<>(
                    new PairWriter<>(
                        ScaleCodecWriter::writeAsList,
                        ScaleCodecWriter::writeAsList)),
                pairs);
        }

        public byte[] encodeAsListOfBytes(Iterable<Byte> bytes) {
            return encodeAsList(ScaleCodecWriter::directWrite, bytes);
        }

        public <T> byte[] encodeAsList(ScaleWriter<T> collectionItemWriter, Iterable<T> values) {
            return encode(new IterableWriter<>(collectionItemWriter), values);
        }

        public byte[] encode(byte[][] values) {
            return encode(new ListWriter<>(ScaleCodecWriter::writeAsList), Arrays.asList(values));
        }

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
