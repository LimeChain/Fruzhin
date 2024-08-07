package com.limechain.utils;

import lombok.experimental.UtilityClass;

import java.security.InvalidParameterException;
import java.util.regex.Pattern;

@UtilityClass
public class StringUtils {
    private static final Pattern HEX_PATTERN = Pattern.compile("^(0x)?[0-9a-fA-F]+$");
    public static final String HEX_PREFIX = "0x";

    /**
     * Converts a hexadecimal string into an array of bytes.
     * The method first checks if the hex string has an even length and if it matches the hex pattern.
     * It then removes the "0x" prefix if present and converts the cleaned hex string to a byte array.
     *
     * @param hex the hexadecimal string to convert
     * @return the corresponding byte array
     * @throws InvalidParameterException if the hex string has an odd length or does not match the hex pattern
     */
    public static byte[] hexToBytes(String hex) {
        if (hex.length() % 2 != 0) {
            throw new InvalidParameterException("Invalid hex string length");
        }

        if (!HEX_PATTERN.matcher(hex).matches()) {
            throw new InvalidParameterException("Invalid hex string");
        }

        // Trim the 0x prefix if it exists
        hex = remove0xPrefix(hex);

//        return ByteString.fromHex(hex).toByteArray();
        return null;
    }

    /**
     * Removes the "0x" prefix from a hexadecimal string, if it exists.
     * This is useful for cleaning up hex strings to ensure they can be processed or parsed elsewhere.
     *
     * @param hex the hexadecimal string from which the prefix should be removed
     * @return the hex string without the "0x" prefix
     */
    public static String remove0xPrefix(String hex) {
        if (hex.startsWith(HEX_PREFIX)) {
            return hex.substring(2);
        }
        return hex;
    }

    /**
     * Converts a string to its hexadecimal representation.
     * Each character of the input string is converted to its corresponding two-digit hex value.
     *
     * @param key the string to convert to hexadecimal
     * @return the hexadecimal representation of the input string
     */
    public static String toHex(String key) {
        StringBuilder sb = new StringBuilder();
        char[] ch = key.toCharArray();
        for (char c : ch) {
            String hexString = Integer.toHexString(c);
            sb.append(hexString);
        }
        return sb.toString();
    }

    /**
     * Converts a byte array to a hexadecimal string with a "0x" prefix.
     * This is commonly used to represent binary data in a readable hex format prefixed to indicate its base.
     *
     * @param bytes the byte array to convert
     * @return the "0x" prefixed hexadecimal string
     */
    public static String toHexWithPrefix(byte[] bytes) {
        return HEX_PREFIX + "Hex.toHexString(bytes)";
    }
}
