package com.limechain.utils;

import com.google.protobuf.ByteString;

import java.security.InvalidParameterException;
import java.util.regex.Pattern;

public class StringUtils {
    private static final Pattern hexPattern = Pattern.compile("^(0x)?[0-9a-fA-F]+$");

    public static byte[] hexToBytes(String hex) {
        if (hex.length() % 2 != 0) {
            throw new InvalidParameterException("Invalid hex string length");
        }

        if (!hexPattern.matcher(hex).matches()) {
            throw new InvalidParameterException("Invalid hex string");
        }

        // Trim the 0x prefix if it exists
        if (hex.startsWith("0x")) {
            hex = hex.substring(2);
        }
        
        return ByteString.fromHex(hex).toByteArray();
    }
}
