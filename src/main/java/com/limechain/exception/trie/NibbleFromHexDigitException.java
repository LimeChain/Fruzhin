package com.limechain.exception.trie;

public class NibbleFromHexDigitException extends NibbleException {

    public NibbleFromHexDigitException(String message) {
        super(message);
    }

    public static NibbleFromHexDigitException invalidHexDigit(char c) {
        return new NibbleFromHexDigitException(
            String.format("Invalid hexadecimal digit character '%c' given to construct a Nibble.", c));
    }
}
