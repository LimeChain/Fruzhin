package com.limechain.utils;

import com.google.protobuf.ByteString;

import java.security.InvalidParameterException;

public class StringUtils {
    public static byte[] hexToBytes(String hex) {
        // Trim the 0x prefix if it exists
        if (hex.startsWith("0x")) {
            hex = hex.substring(2);
        }

        if (hex.length() % 2 != 0) {
            throw new InvalidParameterException("Invalid hex string length");
        }

        return ByteString.fromHex(hex).toByteArray();
    }
}
