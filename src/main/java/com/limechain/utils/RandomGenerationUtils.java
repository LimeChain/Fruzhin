package com.limechain.utils;

import java.util.Random;

public class RandomGenerationUtils {
    public static byte[] generateBytes(int length) {
        Random generator = new Random(0);
        byte[] bytes = new byte[length];
        generator.nextBytes(bytes);
        return bytes;
    }

}
