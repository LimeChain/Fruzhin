package com.limechain.utils;

public class ByteArrayUtils {
    public static int commonPrefixLength(byte[] a, byte[] b) {
        int minLength = Math.min(a.length, b.length);
        int length = 0;
        for (length = 0; length < minLength; length++) {
            if (a[length] != b[length]) {
                break;
            }
        }
        return length;
    }

    public static boolean hasPrefix(byte[] array, byte[] prefix) {
        if (array.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (array[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }
}
