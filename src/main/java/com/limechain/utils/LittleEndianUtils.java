package com.limechain.utils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class LittleEndianUtils {
    public static byte[] convertBytes(byte[] byteArray) {
        // Create a ByteBuffer and set its order to little-endian
        ByteBuffer buffer = ByteBuffer.wrap(byteArray);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        // Convert the bytes to little-endian
        byte[] littleEndianBytes = new byte[buffer.remaining()];
        buffer.get(littleEndianBytes);

        return littleEndianBytes;
    }

    public static byte[] bytesToFixedLength(byte[] byteArray, int length) {
        byte[] littleEndian = new byte[length];

        for (int i = 0; i < byteArray.length; i++) {
            littleEndian[i] = byteArray[byteArray.length - 1 - i];
        }

        return littleEndian;
    }

    public static byte[] intTo32LEBytes(int number) {
        byte byte1 = (byte) (number);
        byte byte2 = (byte) (number >>> 8);
        byte byte3 = (byte) (number >>> 16);
        byte byte4 = (byte) (number >>> 24);
        return new byte[]{byte1, byte2, byte3, byte4};
    }
}
