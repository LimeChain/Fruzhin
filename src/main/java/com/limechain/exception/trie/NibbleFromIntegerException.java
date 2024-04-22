package com.limechain.exception.trie;

public class NibbleFromIntegerException extends NibbleException {

    public NibbleFromIntegerException(String message) {
        super(message);
    }

    public static NibbleFromIntegerException valueNegative(int value) {
        return new NibbleFromIntegerException(
            String.format("Integer value %d is negative, can't fit into a nibble.", value));
    }

    public static NibbleFromIntegerException valueTooLarge(int value) {
        return new NibbleFromIntegerException(
            String.format("Integer value %d is too large (>= 16) to fit into a nibble.", value));
    }
}
