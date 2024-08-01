package com.limechain.trie.cache;

import com.limechain.trie.cache.node.PendingInsertUpdate;
import com.limechain.trie.cache.node.PendingTrieNodeChange;
import com.limechain.trie.structure.nibble.Nibble;
import com.limechain.trie.structure.nibble.Nibbles;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class TrieChangesTest {

    private TrieChanges trieChanges;
    private TreeMap<Nibbles, PendingTrieNodeChange> changes;

    @BeforeEach
    void setUp() {
        trieChanges = TrieChanges.empty();
        changes = trieChanges.getChanges();
    }

    @Test
    void testGetRoot_Empty() {
        assertEquals(Optional.empty(), trieChanges.getRoot());
    }

    @Test
    void testGetRoot_NonEmpty() {
        Nibbles key = Nibbles.fromBytes("root".getBytes());
        PendingInsertUpdate rootChange = mock(PendingInsertUpdate.class);
        changes.put(key, rootChange);

        Optional<PendingInsertUpdate> root = trieChanges.getRoot();
        assertTrue(root.isPresent());
        assertEquals(rootChange, root.get());
    }

    @Test
    void testGetEntriesInKeyPath_NoClassFilter() {
        Nibbles key1 = Nibbles.fromHexString("1234");
        Nibbles key2 = Nibbles.fromHexString("12345678");
        PendingTrieNodeChange update1 = mock(PendingTrieNodeChange.class);
        PendingTrieNodeChange update2 = mock(PendingTrieNodeChange.class);
        changes.put(key1, update1);
        changes.put(key2, update2);

        List<Map.Entry<Nibbles, PendingTrieNodeChange>> entries = trieChanges.getEntriesInKeyPath(
            null, Nibbles.fromHexString("123456789"));
        assertEquals(2, entries.size());
        assertEquals(update1, entries.get(0).getValue());
        assertEquals(update2, entries.get(1).getValue());
    }

    @Test
    void testGetEntriesInKeyPath_WithClassFilter() {
        Nibbles key1 = Nibbles.fromHexString("1234");
        Nibbles key2 = Nibbles.fromHexString("12345678");
        PendingInsertUpdate update1 = mock(PendingInsertUpdate.class);
        PendingTrieNodeChange update2 = mock(PendingTrieNodeChange.class);
        changes.put(key1, update1);
        changes.put(key2, update2);

        List<Map.Entry<Nibbles, PendingInsertUpdate>> entries = trieChanges.getEntriesInKeyPath(
            PendingInsertUpdate.class, Nibbles.fromHexString("123456789"));
        assertEquals(1, entries.size());
        assertEquals(update1, entries.get(0).getValue());
    }

    @Test
    void testGetChildByIndex_NotFound() {
        Nibbles parentKey = Nibbles.fromHexString("123");
        Nibble childIndex = Nibble.fromAsciiHexDigit('4');

        assertEquals(Optional.empty(), trieChanges.getChildByIndex(parentKey, childIndex));
    }

    @Test
    void testGetChildByIndex_Found() {
        Nibbles parentKey = Nibbles.fromHexString("123");
        Nibble childIndex = Nibble.fromAsciiHexDigit('4');
        Nibbles childKey = parentKey.add(childIndex);
        PendingInsertUpdate childChange = mock(PendingInsertUpdate.class);
        changes.put(childKey, childChange);

        Optional<PendingInsertUpdate> child = trieChanges.getChildByIndex(parentKey, childIndex);
        assertTrue(child.isPresent());
        assertEquals(childChange, child.get());
    }
}