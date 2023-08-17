package com.limechain.utils;

import lombok.experimental.UtilityClass;

@UtilityClass
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

    /**
     * Returns the start position of the first occurrence of the specified {@code target} within
     * {@code array}, or {@code -1} if there is no such occurrence.
     *
     * <p>More formally, returns the lowest index {@code i} such that {@code Arrays.copyOfRange(array,
     * i, i + target.length)} contains exactly the same elements as {@code target}.
     *
     * @param array the array to search for the sequence {@code target}
     * @param target the array to search for as a sub-sequence of {@code array}
     */
    public static int indexOf(byte[] array, byte[] target) {
        if (array == null || target == null){
            return -1;
        }
        if (target.length == 0) {
            return 0;
        }

        outer:
        for (int i = 0; i < array.length - target.length + 1; i++) {
            for (int j = 0; j < target.length; j++) {
                if (array[i + j] != target[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }
}
