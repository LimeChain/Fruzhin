package com.limechain.trie;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class NibblesTest {

    @Test
    void testNibblesToKeyLE() {
        // Test cases
        byte[][] nibbles = {
                {},                                       // empty_nibbles
                {0xF, 0xF},                               // 0xF_0xF
                {0x3, 0xa, 0x0, 0x5},                     // 0x3_0xa_0x0_0x5
                {0xa, 0xa, 0xf, 0xf, 0x0, 0x1},           // 0xa_0xa_0xf_0xf_0x0_0x1
                {0xa, 0xa, 0xf, 0xf, 0x0, 0x1, 0xc, 0x2}, // 0xa_0xa_0xf_0xf_0x0_0x1_0xc_0x2
                {0xa, 0xa, 0xf, 0xf, 0x0, 0x1, 0xc}       // 0xa_0xa_0xf_0xf_0x0_0x1_0xc
        };

        byte[][] keyLE = {
                {},                                            // empty_nibbles
                {(byte) 0xFF},                                 // 0xF_0xF
                {0x3a, 0x05},                                  // 0x3_0xa_0x0_0x5
                {(byte) 0xaa, (byte) 0xff, 0x01},              // 0xa_0xa_0xf_0xf_0x0_0x1
                {(byte) 0xaa, (byte) 0xff, 0x01, (byte) 0xc2}, // 0xa_0xa_0xf_0xf_0x0_0x1_0xc_0x2
                {0xa, (byte) 0xaf, (byte) 0xf0, 0x1c}          // 0xa_0xa_0xf_0xf_0x0_0x1_0xc
        };

        // Iterate over test cases
        for (int i = 0; i < nibbles.length; i++) {
            byte[] result = Nibbles.nibblesToKeyLE(nibbles[i]);
            assertArrayEquals(keyLE[i], result);
        }
    }

    @Test
    void testKeyLEToNibbles() {
        // Test cases
        byte[][] keyLE = {
                {},                                            // empty_input
                {0x0},                                         // 0x0
                {(byte) 0xFF},                                 // 0xFF
                {0x3a, 0x05},                                  // 0x3a_0x05
                {(byte) 0xAA, (byte) 0xFF, 0x01},              // 0xAA_0xFF_0x01
                {(byte) 0xAA, (byte) 0xFF, 0x01, (byte) 0xc2}, // 0xAA_0xFF_0x01_0xc2
                {(byte) 0xAA, (byte) 0xFF, 0x01, (byte) 0xc0}  // 0xAA_0xFF_0x01_0xc0
        };

        byte[][] nibbles = {
                {},                                       // empty_input
                {0, 0},                                   // 0x0
                {0xF, 0xF},                               // 0xFF
                {0x3, 0xa, 0x0, 0x5},                     // 0x3a_0x05
                {0xa, 0xa, 0xf, 0xf, 0x0, 0x1},           // 0xAA_0xFF_0x01
                {0xa, 0xa, 0xf, 0xf, 0x0, 0x1, 0xc, 0x2}, // 0xAA_0xFF_0x01_0xc2
                {0xa, 0xa, 0xf, 0xf, 0x0, 0x1, 0xc, 0x0}  // 0xAA_0xFF_0x01_0xc0
        };

        // Iterate over test cases
        for (int i = 0; i < keyLE.length; i++) {
            byte[] result = Nibbles.keyLEToNibbles(keyLE[i]);
            assertArrayEquals(nibbles[i], result);
        }
    }

    @Test
    void testNibblesKeyLE() {
        // Test cases
        byte[][] nibblesToEncode = {
                {},                 // empty_input
                {1},                // one_byte
                {1, 2},             // two_bytes
                {1, 2, 3}           // three_bytes
        };

        byte[][] nibblesDecoded = {
                {},                 // empty_input
                {0, 1},             // one_byte
                {1, 2},             // two_bytes
                {0, 1, 2, 3}        // three_bytes
        };

        // Iterate over test cases
        for (int i = 0; i < nibblesToEncode.length; i++) {
            byte[] keyLE = Nibbles.nibblesToKeyLE(nibblesToEncode[i]);
            byte[] result = Nibbles.keyLEToNibbles(keyLE);
            assertArrayEquals(nibblesDecoded[i], result);
        }
    }
}