package com.limechain.utils.scale.writers;

import io.emeraldpay.polkaj.scale.ScaleCodecWriter;
import io.emeraldpay.polkaj.scale.ScaleWriter;
import org.apache.commons.collections4.IterableUtils;

import java.io.IOException;
import java.util.Collection;

public class CollectionWriter<T> implements ScaleWriter<Collection<T>> {
    private final ScaleWriter<T> scaleWriter;

    public CollectionWriter(ScaleWriter<T> scaleWriter) {
        this.scaleWriter = scaleWriter;
    }
    @Override
    public void write(ScaleCodecWriter scaleCodecWriter, Collection<T> values) throws IOException {
        scaleCodecWriter.writeCompact(values.size());

        for (T item : values) {
            this.scaleWriter.write(scaleCodecWriter, item);
        }
    }
}
