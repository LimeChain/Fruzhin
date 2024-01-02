package com.limechain.utils;

import org.junit.jupiter.api.Test;

import java.security.InvalidParameterException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StringUtilsTest {

    @Test
    void decodesPrefixedHexString() {
        String hexString = "0x0102030405060708090a0b0c0d0e0f";
        byte[] expected = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9,
                10, 11, 12, 13, 14, 15};
        byte[] actual = StringUtils.hexToBytes(hexString);
        assertArrayEquals(expected, actual);
    }

    @Test
    void decodesNonPrefixedHexString() {
        String hexString = "0102030405060708090a0b0c0d0e0f";
        byte[] expected = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9,
                10, 11, 12, 13, 14, 15};
        byte[] actual = StringUtils.hexToBytes(hexString);
        assertArrayEquals(expected, actual);
    }

    @Test
    void decodeFailsIfLengthIsOddPrefixed() {
        String hexString = "0x0102030405060708090a0b0c0d0e0";
        assertThrows(InvalidParameterException.class, () -> StringUtils.hexToBytes(hexString));
    }

    @Test
    void decodeFailsIfLengthIsOddNotPrefixed() {
        String hexString = "0102030405060708090a0b0c0d0e0";
        assertThrows(InvalidParameterException.class, () -> StringUtils.hexToBytes(hexString));
    }
}