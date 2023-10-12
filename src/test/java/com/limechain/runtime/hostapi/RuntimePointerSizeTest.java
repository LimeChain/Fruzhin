package com.limechain.runtime.hostapi;

import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RuntimePointerSizeTest {
    int sizeBytes = 0b11100100111100111011000001001011;
    int pointerBytes = 0b01011101100111101110110100100111;
    long pointerSize = 0b1110010011110011101100000100101101011101100111101110110100100111L;

    @Test
    void getPointerSizeFromPointerAndSize() {
        long result = new RuntimePointerSize(pointerBytes, sizeBytes).pointerSize();
        assertEquals(pointerSize, result);
    }

    @Test
    void getPointerSizeFromNumber() {
        long result = new RuntimePointerSize(BigInteger.valueOf(pointerSize)).pointerSize();
        assertEquals(pointerSize, result);
    }

    @Test
    void getPointerFromPointerSize() {
        int result = new RuntimePointerSize(pointerSize).pointer();
        assertEquals(pointerBytes, result);
    }

    @Test
    void getSizeFromPointerSize() {
        int result = new RuntimePointerSize(pointerSize).size();
        assertEquals(sizeBytes, result);
    }
}