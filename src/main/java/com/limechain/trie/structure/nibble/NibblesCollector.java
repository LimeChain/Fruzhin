package com.limechain.trie.structure.nibble;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

public class NibblesCollector implements Collector<Nibble, List<Nibble>, Nibbles> {
    public static NibblesCollector toNibbles() {
        return new NibblesCollector();
    }

    @Override
    public Supplier<List<Nibble>> supplier() {
        return LinkedList::new;
    }

    @Override
    public BiConsumer<List<Nibble>, Nibble> accumulator() {
        return List::add;
    }

    @Override
    public BinaryOperator<List<Nibble>> combiner() {
        return (list1, list2) -> {
            list1.addAll(list2);
            return list1;
        };
    }

    @Override
    public Function<List<Nibble>, Nibbles> finisher() {
        return Nibbles::new;
    }

    @Override
    public Set<Characteristics> characteristics() {
        // TODO: Think about what characteristics might be useful, we're going safe for now
        return Set.of();
    }
}
