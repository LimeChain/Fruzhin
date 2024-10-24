package com.limechain.utils;

import lombok.experimental.UtilityClass;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

@UtilityClass
public class LittleEndianUtils {

    /**
     * Converts the given byte array to little-endian format.
     *
     * @param byteArray The byte array to be converted.
     * @return A new byte array representing the input byte array in little-endian format.
     */
    public static byte[] convertBytes(byte[] byteArray) {
        // Create a ByteBuffer and set its order to little-endian
        ByteBuffer buffer = ByteBuffer.wrap(byteArray);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        // Convert the bytes to little-endian
        byte[] littleEndianBytes = new byte[buffer.remaining()];
        buffer.get(littleEndianBytes);

        return littleEndianBytes;
    }

    /**
     * Converts the given byte array to a fixed length little-endian byte array.
     * If the input array is shorter than the specified length, it will be padded with zeros.
     *
     * @param byteArray The byte array to be converted to fixed length little-endian.
     * @param length The desired length of the resulting byte array.
     * @return A new byte array with the little-endian representation of the input, padded with zeros if necessary.
     */
    public static byte[] bytesToFixedLength(byte[] byteArray, int length) {
        byte[] littleEndian = new byte[length];
        int smallestLength = Math.min(byteArray.length, littleEndian.length);

        for(int i = 0; i < smallestLength; i++) {
            littleEndian[i] = byteArray[byteArray.length - 1 - i];
        }

        return littleEndian;
    }

    /**
     * Converts the given integer to a little-endian byte array of length 4 (32 bits).
     *
     * @param number The integer to be converted.
     * @return A new byte array representing the little-endian bytes of the input integer.
     */
    public static byte[] intTo32LEBytes(int number) {
        byte byte1 = (byte) (number);
        byte byte2 = (byte) (number >>> 8);
        byte byte3 = (byte) (number >>> 16);
        byte byte4 = (byte) (number >>> 24);
        return new byte[]{byte1, byte2, byte3, byte4};
    }

    //TODO: add documentation
    public static byte[] longToLittleEndianBytes(long value) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putLong(value);
        return buffer.array();
    }

    //TODO: add documentation
    public static BigInteger fromLittleEndianByteArray(byte[] bytes) {
        if (bytes.length != 16) {
            throw new IllegalArgumentException("Byte array must be exactly 16 bytes long");
        }

        byte[] reversed = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            reversed[i] = bytes[bytes.length - 1 - i];
        }

        return new BigInteger(1, reversed);
    }
}
