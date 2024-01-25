package com.limechain.utils.scale.writers;

import io.emeraldpay.polkaj.scale.ScaleCodecWriter;
import io.emeraldpay.polkaj.scale.ScaleWriter;
import lombok.AllArgsConstructor;
import org.javatuples.Pair;

import java.io.IOException;

@AllArgsConstructor
public class PairWriter<K, V> implements ScaleWriter<Pair<K, V>> {
    protected ScaleWriter<K> firstWriter;
    protected ScaleWriter<V> secondWriter;

    @Override
    public void write(ScaleCodecWriter scaleCodecWriter, Pair<K, V> pair) throws IOException {
        scaleCodecWriter.write(firstWriter, pair.getValue0());
        scaleCodecWriter.write(secondWriter, pair.getValue1());
    }
}
