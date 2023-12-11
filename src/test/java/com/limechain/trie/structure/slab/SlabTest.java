package com.limechain.trie.structure.slab;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SlabTest {
    @Test
    void test_Simple_Retrieval_Works() {
        Slab<String> slab = new Slab<>();
        String s = "alo";
        int i = slab.add(s);
        assertEquals(s, slab.get(i));
    }

    @Test
    void test_Simple_Removal_Works() {
        Slab<String> slab = new Slab<>();
        String s = "alo";
        int i = slab.add(s);
        assertEquals(s, slab.get(i));
        slab.remove(i);
        assertNull(slab.get(i));
    }
}