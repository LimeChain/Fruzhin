package com.limechain.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ByteArrayUtilsTest {
    @Test
    public void testTargetFound() {
        // Test with both arrays non-null and target found
        byte[] array = {1, 2, 3, 4, 5};
        byte[] target = {3, 4};
        assertEquals(2, ByteArrayUtils.indexOf(array, target));
    }

    @Test
    public void testTargetNotFound() {
        // Test with both arrays non-null and target not found
        byte[] array = {1, 2, 3, 4, 5};
        byte[] target = {6, 7};
        assertEquals(-1, ByteArrayUtils.indexOf(array, target));
    }

    @Test
    public void testEmptyTarget() {
        // Test with target array empty
        byte[] array = {1, 2, 3, 4, 5};
        byte[] target = {};
        assertEquals(0, ByteArrayUtils.indexOf(array, target));
    }

    @Test
    public void testArrayNull() {
        // Test with array null
        byte[] target = {1, 2};
        assertEquals(-1, ByteArrayUtils.indexOf(null, target));
    }

    @Test
    public void testTargetNull() {
        // Test with target null
        byte[] array = {1, 2, 3, 4, 5};
        assertEquals(-1, ByteArrayUtils.indexOf(array, null));
    }

    @Test
    public void testBothNull() {
        // Test with both arrays null
        assertEquals(-1, ByteArrayUtils.indexOf(null, null));
    }
}