package com.limechain.trie.structure;

import com.limechain.trie.structure.nibble.Nibbles;
import lombok.AllArgsConstructor;

/**
 * A class used to represent the result of querying the trie structure for "what's there at this particular full key",
 * i.e. the return type of {@link TrieStructure#node(Nibbles key)}.
 * <br>
 * <br>
 * An {@link Entry} could either be:
 * <ul>
 *     <li>{@link Vacant}, meaning no node corresponds to this full key (nibble path), or</li>
 *     <li>a {@link NodeHandle}, indicating that a node already exists at that path.</li>
 * </ul>
 */
@AllArgsConstructor
public abstract sealed class Entry<T> permits NodeHandle, Vacant {
    protected final TrieStructure<T> trieStructure;

    /**
     * @return {@code this}, but cast as {@link Vacant}
     * @throws ClassCastException if the underlying Entry is not a Vacant one.
     */
    public Vacant<T> asVacant() {
        return (Vacant<T>) this;
    }

    /**
     * @return {@code this}, but cast as {@link NodeHandle}
     * @throws ClassCastException if the underlying Entry is not a NodeHandle one.
     */
    public NodeHandle<T> asNodeHandle() {
        return (NodeHandle<T>) this;
    }
}
