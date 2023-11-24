package com.limechain.utils.scale.writers;

import io.emeraldpay.polkaj.scale.ScaleCodecWriter;
import io.emeraldpay.polkaj.scale.ScaleWriter;
import kotlin.Pair;
import lombok.AllArgsConstructor;

import java.io.IOException;

@AllArgsConstructor
public class PairWriter<K, V> implements ScaleWriter<Pair<K, V>> {
    protected ScaleWriter<K> firstWriter;
    protected ScaleWriter<V> secondWriter;

    @Override
    public void write(ScaleCodecWriter scaleCodecWriter, Pair<K, V> pair) throws IOException {
        scaleCodecWriter.write(firstWriter, pair.getFirst());
        scaleCodecWriter.write(secondWriter, pair.getSecond());
    }
}
