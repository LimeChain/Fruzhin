package com.limechain.trie.cache;

import com.limechain.trie.cache.node.PendingInsertUpdate;
import com.limechain.trie.cache.node.PendingTrieNodeChange;
import com.limechain.trie.structure.nibble.Nibble;
import com.limechain.trie.structure.nibble.Nibbles;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * Container used to cache the difference between two versions of a trie. For example this can be used when the runtime
 * edits the storage via host api calls, block execution, etc. The aim of this is to lower the number of expensive
 * operations towards an on disk merkle trie.
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class TrieChanges {

    /**
     * Holds data for node changes identified by their path.
     */
    public final TreeMap<Nibbles, PendingTrieNodeChange> changes;

    public static TrieChanges empty() {
        return new TrieChanges(new TreeMap<>());
    }

    public void clear() {
        changes.clear();
    }

    public boolean isKeyInCache(Nibbles key) {
        return changes.get(key) != null;
    }

    public boolean isCacheEmpty() {
        return changes.isEmpty();
    }

    public void updateCache(TreeMap<Nibbles, PendingTrieNodeChange> updates) {
        changes.putAll(updates);
    }

    public Optional<PendingTrieNodeChange> getFromCache(Nibbles key) {
        return Optional.ofNullable(changes.get(key));
    }

    public void removeFromCache(Nibbles key) {
        changes.remove(key);
    }

    public Optional<PendingInsertUpdate> getRoot() {
        Map.Entry<Nibbles, PendingTrieNodeChange> rootChange = changes.firstEntry();
        return rootChange != null
            ? Optional.of((PendingInsertUpdate) rootChange.getValue())
            : Optional.empty();
    }

    public <P extends PendingTrieNodeChange> List<Map.Entry<Nibbles, P>> getEntriesInKeyPath(
        @Nullable Class<P> clazz, Nibbles key) {
        Stream<Map.Entry<Nibbles, PendingTrieNodeChange>> stream = changes.subMap(
            Nibbles.EMPTY, true, key, true).entrySet().stream();

        if (clazz != null) {
            stream = stream
                .filter(e -> clazz.isInstance(e.getValue()));
        }

        return stream
            .filter(e -> key.startsWith(e.getKey()))
            .map(e -> Map.entry(e.getKey(), (P) e.getValue()))
            .toList();
    }

    // TODO 437 optimize so that it doesn't traverse until end of map if missing
    public Optional<PendingInsertUpdate> getChildByIndex(Nibbles parentKey, Nibble childIndex) {
        Nibbles parentKeyWithChildIndex = parentKey.add(childIndex);
        return changes.tailMap(parentKey, false).entrySet().stream()
            .filter(e -> e.getKey().startsWith(parentKeyWithChildIndex) && e.getValue() instanceof PendingInsertUpdate)
            .map(e -> (PendingInsertUpdate) e.getValue())
            .findFirst();
    }
}
