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
}
