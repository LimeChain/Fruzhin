package com.limechain.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ByteArrayUtilsTest {
    @Test
    void testTargetFound() {
        // Test with both arrays non-null and target found
        byte[] array = {1, 2, 3, 4, 5};
        byte[] target = {3, 4};
        assertEquals(2, ByteArrayUtils.indexOf(array, target));
    }

    @Test
    void testTargetNotFound() {
        // Test with both arrays non-null and target not found
        byte[] array = {1, 2, 3, 4, 5};
        byte[] target = {6, 7};
        assertEquals(-1, ByteArrayUtils.indexOf(array, target));
    }

    @Test
    void testEmptyTarget() {
        // Test with target array empty
        byte[] array = {1, 2, 3, 4, 5};
        byte[] target = {};
        assertEquals(0, ByteArrayUtils.indexOf(array, target));
    }

    @Test
    void testArrayNull() {
        // Test with array null
        byte[] target = {1, 2};
        assertEquals(-1, ByteArrayUtils.indexOf(null, target));
    }

    @Test
    void testTargetNull() {
        // Test with target null
        byte[] array = {1, 2, 3, 4, 5};
        assertEquals(-1, ByteArrayUtils.indexOf(array, null));
    }

    @Test
    void testBothNull() {
        // Test with both arrays null
        assertEquals(-1, ByteArrayUtils.indexOf(null, null));
    }
}