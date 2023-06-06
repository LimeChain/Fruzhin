package com.limechain.trie;

public class Nibbles {
    /**
     * Converts a key in nibbles format to LE format. The key is assumed to be in nibbles format.
     *
     * @param nibbles key in nibbles format
     * @return key in LE format
     */
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

    /**
     * Converts a key in LE format to nibbles. The key is assumed to be in LE format.
     *
     * @param in key in LE format
     * @return nibbles representation of the key
     */
    public static byte[] keyLEToNibbles(byte[] in) {
        if (in.length == 0) {
            return new byte[]{};
        } else if (in.length == 1 && in[0] == 0) {
            return new byte[]{0, 0};
        }

        int l = in.length * 2;
        byte[] nibbles = new byte[l];
        for (int i = 0; i < in.length; i++) {
            nibbles[2 * i] = (byte) (in[i] >> 4 & 0xf);
            nibbles[2 * i + 1] = (byte) (in[i] & 0xf);
        }

        return nibbles;
    }
}
