package com.limechain.trie.structure.nibble;

import com.limechain.trie.structure.nibble.exceptions.NibbleFromHexDigitException;
import com.limechain.trie.structure.nibble.exceptions.NibbleFromIntegerException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NibbleTest {
    @Test
    void properConstructionWorks() {
        // From int / byte
        assertDoesNotThrow(() -> {
            for (int i = 0; i < 16; ++i) {
                var n1 = Nibble.fromByte((byte) i);
                var n2 = Nibble.fromInt(i);
                assertEquals(n1, n2);
            }
        });

        // From radix 10 char digits
        assertDoesNotThrow(() -> {
            for (char c = '0'; c <= '9'; ++c) {
                Nibble.fromAsciiHexDigit(c);
            }
        });

        // From radix 16 char digits
        assertDoesNotThrow(() -> {
            List<Nibble> lower = new ArrayList<>(6);
            for (char c = 'a'; c <= 'f'; ++c) {
                lower.add(Nibble.fromAsciiHexDigit(c));
            }

            List<Nibble> upper = new ArrayList<>(6);
            for (char c = 'A'; c <= 'F'; ++c) {
                upper.add(Nibble.fromAsciiHexDigit(c));
            }

            assertEquals(lower, upper);
        });
    }

    @Test
    void improperConstructionThrows() {
        // From integers
        List<Integer> invalidNibbleInts = List.of(
            -1,
            -15,
            -16,
            16,
            100,
            -100
        );

        for (int invalidNibble : invalidNibbleInts) {
            assertThrows(NibbleFromIntegerException.class, () -> {
                Nibble.fromInt(invalidNibble);
            });

            assertThrows(NibbleFromIntegerException.class, () -> {
                Nibble.fromByte((byte) invalidNibble);
            });
        }

        // From chars
        List<Character> invalidNibbleChars = List.of(
            '/',
            ':',
            'ä',
            'Ä',
            'ç'
        );

        for (char invalidNibble : invalidNibbleChars) {
            assertThrows(NibbleFromHexDigitException.class, () -> {
                Nibble.fromAsciiHexDigit(invalidNibble);
            });
        }
    }

    @Test
    void hexDigitRepresentationsWork() {
        char[] lower = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};
        char[] upper = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

        IntStream.range(0, 16).forEach(i -> {
            Nibble nibble = Nibble.fromInt(i);

            assertEquals(lower[i], nibble.asLowerHexDigit(), "Lower hex digit representation mismatch.");
            assertEquals(upper[i], nibble.asUpperHexDigit(), "Upper hex digit representation mismatch.");
        });
    }
}