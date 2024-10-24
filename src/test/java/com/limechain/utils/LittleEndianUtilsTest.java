package com.limechain.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class LittleEndianUtilsTest {
    @Test
    void testBytesToFixedLength() {
        byte[] bigEndianArray = {0x01, 0x02, 0x03, 0x04};
        int targetLength = 4;

        byte[] expectedLittleEndianArray = {0x04, 0x03, 0x02, 0x01};
        byte[] actualLittleEndianArray = LittleEndianUtils.bytesToFixedLength(bigEndianArray, targetLength);
        assertArrayEquals(expectedLittleEndianArray, actualLittleEndianArray);
    }

    @Test
    void testBytesToFixedLengthWithPadding() {
        byte[] bigEndianArray = {0x0A, 0x0B, 0x0C};
        int targetLength = 5;

        byte[] expectedLittleEndianArray = {0x0C, 0x0B, 0x0A, 0x00, 0x00};
        byte[] actualLittleEndianArray = LittleEndianUtils.bytesToFixedLength(bigEndianArray, targetLength);
        assertArrayEquals(expectedLittleEndianArray, actualLittleEndianArray);
    }

    @Test
    void testBytesToFixedLengthWithTruncation() {
        byte[] bigEndianArray = {0x10, 0x20, 0x30, 0x40, 0x50, 0x60};
        int targetLength = 4;

        byte[] expectedLittleEndianArray = {0x60, 0x50, 0x40, 0x30};
        byte[] actualLittleEndianArray = LittleEndianUtils.bytesToFixedLength(bigEndianArray, targetLength);
        assertArrayEquals(expectedLittleEndianArray, actualLittleEndianArray);
    }
}
