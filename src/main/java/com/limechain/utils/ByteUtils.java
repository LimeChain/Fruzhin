package com.limechain.utils;

public class ByteUtils {
    public static byte[] convertIntArrayToByteArray(int[] intArray) {
        byte[] byteArray = new byte[intArray.length * 4]; // Each integer is 4 bytes

        for (int i = 0; i < intArray.length; i++) {
            int value = intArray[i];
            byteArray[i * 4] = (byte) (value >> 24);
            byteArray[i * 4 + 1] = (byte) (value >> 16);
            byteArray[i * 4 + 2] = (byte) (value >> 8);
            byteArray[i * 4 + 3] = (byte) value;
        }

        return byteArray;
    }

}
