package com.limechain.trie;

import com.limechain.trie.structure.database.NodeData;
import com.limechain.trie.structure.nibble.Nibbles;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public class TrieDiff {
    private final NavigableMap<Nibbles, Boolean> availability = new TreeMap<>(Comparator.comparing(Nibbles::toString));
    private final Map<Nibbles, NodeData> difference = new HashMap<>();

    public void clear() {
        this.difference.clear();
        this.availability.clear();
    }

    public NodeData diffInsert(Nibbles key, NodeData userData) {
        // Previous value, if any, will be replaced.
        NodeData previous = this.difference.put(key, userData);
        // Mark this key as updated (true) in the availability map.
        this.availability.put(key, true);

        return previous;
    }

    public NodeData diffInsertErase(Nibbles key) {
        // Mark the key as erased (false) in the availability map.
        this.availability.put(key, false);
        // Remove the key from the difference map and return the previous value, if any.
        return this.difference.put(key, null);
    }

    public NodeData diffRemove(Nibbles key) {
        // Remove the entry from both maps and return the previous value, if any.
        this.availability.remove(key);
        return this.difference.remove(key);
    }

    public NodeData get(Nibbles key) {
        // Retrieve the value associated with the key from the difference map.
        return this.difference.get(key);
    }

    public boolean isAvailable(Nibbles key) {
        return this.availability.getOrDefault(key, false);
    }

    public boolean isDeleted(Nibbles key) {
        return this.availability.containsKey(key) && !availability.get(key);
    }

    // Iterates through all entries in an unordered manner.
    public Map<Nibbles, NodeData> diffIterUnordered() {
        return new TreeMap<>(difference);
    }

    // Iterates through all entries within a given range in an ordered manner.
    public Map<Nibbles, Boolean> diffRangeOrdered(Nibbles start, Nibbles end) {
        return availability.subMap(start, true, end, true);
    }


    public void merge(TrieDiff other) {
        other.difference.forEach((key, value) -> {
            this.difference.put(key, value);
            this.availability.put(key, true);
        });
        other.availability.forEach(this.availability::putIfAbsent);
    }

    public Nibbles storageNextKey(Nibbles key, Nibbles inParentNextKey, boolean orEqual) {
        return null; // Placeholder for the actual logic.
    }
}
