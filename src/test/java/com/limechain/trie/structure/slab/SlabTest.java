package com.limechain.trie.structure.slab;

import org.javatuples.Pair;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SlabTest {
    @Test
    void addAndGetTest() {
        Slab<String> slab = new Slab<>();
        int index = slab.add("test");
        assertEquals(0, index);
        assertEquals("test", slab.get(index));
    }

    @Test
    void removeTest() {
        Slab<String> slab = new Slab<>();
        int index = slab.add("test");
        assertEquals("test", slab.remove(index));
        assertNull(slab.get(index));
    }

    @Test
    void clearTest() {
        Slab<String> slab = new Slab<>();
        slab.add("test1");
        slab.add("test2");
        slab.clear();
        assertTrue(slab.isEmpty());
    }

    @Test
    void isEmptyTest() {
        Slab<String> slab = new Slab<>();
        assertTrue(slab.isEmpty());
        slab.add("test");
        assertFalse(slab.isEmpty());
    }

    @Test
    void sizeTest() {
        Slab<String> slab = new Slab<>();
        slab.add("test1");
        slab.add("test2");
        assertEquals(2, slab.size());
    }

    @Test
    void drainTest() {
        Slab<String> slab = new Slab<>();
        slab.add("test1");
        slab.add("test2");
        List<Pair<Integer, String>> drained = slab.drain();
        assertEquals(2, drained.size());
        assertTrue(slab.isEmpty());
    }

    @Test
    void iteratorTest() {
        Slab<String> slab = new Slab<>();
        slab.add("test1");
        slab.add("test2");
        Iterator<org.javatuples.Pair<Integer, String>> it = slab.iterator();
        assertTrue(it.hasNext());
        assertNotNull(it.next());
        assertTrue(it.hasNext());
        assertNotNull(it.next());
        assertFalse(it.hasNext());
    }

    @Test
    void initialCapacityIncreaseTest() {
        int initialCapacity = 10;
        Slab<String> slab = new Slab<>(initialCapacity);

        for (int i = 0; i < initialCapacity; i++) {
            slab.add("test " + i);
        }

        assertEquals(initialCapacity, slab.size());

        slab.add("Extra Element");
        assertEquals(initialCapacity + 1, slab.size());
    }

    @Test
    void removeNonExistentElementThrowsErrorTest() {
        Slab<String> slab = new Slab<>();

        slab.add("test1");
        slab.add("test2");

        assertThrows(IndexOutOfBoundsException.class, () -> {
            slab.remove(5);
        });

        assertEquals(2, slab.size());
    }

    @Test
    void removeNegativeIndexThrowsExceptionTest() {
        Slab<String> slab = new Slab<>();

        assertThrows(IndexOutOfBoundsException.class, () -> {
            slab.remove(-1);
        });
    }

    @Test
    void reuseIndexTest() {
        Slab<String> slab = new Slab<>();

        int initialIndex = slab.add("test1");
        slab.remove(initialIndex);

        int reusedIndex = slab.add("test2");
        assertEquals(initialIndex, reusedIndex);

        assertEquals("test2", slab.get(reusedIndex));
    }

    @Test
    void emptySlabIteratorIsEmptyTest() {
        Slab<String> slab = new Slab<>();

        Iterator<Pair<Integer, String>> iterator = slab.iterator();

        assertFalse(iterator.hasNext());
    }

    @Test
    void testIteratorOverElements() {
        Slab<String> slab = new Slab<>();
        List<String> addedElements = new ArrayList<>();

        addedElements.add("test1");
        slab.add("test1");
        addedElements.add("test2");
        slab.add("test2");

        slab.remove(0);
        addedElements.remove(0);

        List<String> iteratedElements = new ArrayList<>();
        for (Pair<Integer, String> pair : slab) {
            iteratedElements.add(pair.getValue1());
        }

        assertEquals(addedElements, iteratedElements);
    }

    @Test
    void testConsecutiveAddsAndRemoves() {
        Slab<String> slab = new Slab<>();
        Set<Integer> addedIndices = new HashSet<>();

        for (int i = 0; i < 5; i++) {
            int index = slab.add("test" + i);
            addedIndices.add(index);
        }

        slab.remove(1);
        slab.remove(3);
        addedIndices.remove(1);
        addedIndices.remove(3);

        for (int i = 5; i < 7; i++) {
            int index = slab.add("test" + i);
            assertTrue(addedIndices.add(index));
        }

        assertEquals(addedIndices.size(), slab.size());

        for (Integer index : addedIndices) {
            assertNotNull(slab.get(index));
        }
    }
}