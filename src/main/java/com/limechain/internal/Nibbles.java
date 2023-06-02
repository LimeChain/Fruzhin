package com.limechain.internal;

public class Nibbles {
    //TODO: Add docs
    public static byte[] nibblesToKeyLE(byte[] nibbles) {
        if (nibbles.length % 2 == 0) {
            byte[] keyLE = new byte[nibbles.length / 2];
            for (int i = 0; i < nibbles.length; i += 2) {
                keyLE[i / 2] = (byte) ((nibbles[i] << 4 & 0xf0) | (nibbles[i + 1] & 0xf));
            }
            return keyLE;
        }

        byte[] keyLE = new byte[nibbles.length / 2 + 1];
        keyLE[0] = nibbles[0];
        for (int i = 2; i < nibbles.length; i += 2) {
            keyLE[i / 2] = (byte) ((nibbles[i - 1] << 4 & 0xf0) | (nibbles[i] & 0xf));
        }

        return keyLE;
    }

    //TODO: Add docs
    public static byte[] keyLEToNibbles(byte[] in) {
        if (in.length == 0) {
            return new byte[]{};
        } else if (in.length == 1 && in[0] == 0) {
            return new byte[]{0, 0};
        }

        int l = in.length * 2;
        byte[] nibbles = new byte[l];
        for (int i = 0; i < in.length; i++) {
            nibbles[2 * i] = (byte) (in[i] / 16);
            nibbles[2 * i + 1] = (byte) (in[i] % 16);
        }

        return nibbles;
    }
}
