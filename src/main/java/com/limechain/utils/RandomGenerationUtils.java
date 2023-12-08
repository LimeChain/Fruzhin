package com.limechain.utils;

import com.limechain.network.Network;
import io.ipfs.multiaddr.MultiAddress;
import lombok.experimental.UtilityClass;

import java.util.Random;

@UtilityClass
public class RandomGenerationUtils {
    private static final Random RANDOM = new Random();
    
    public static byte[] generateBytes(int length) {
        Random generator = new Random(0);
        byte[] bytes = new byte[length];
        generator.nextBytes(bytes);
        return bytes;
    }

    private static int generateRandomPort(){
        return 10000 + RANDOM.nextInt(50000);
    }

    public static MultiAddress generateRandomAddress(){
        return new MultiAddress(Network.LOCAL_IPV4_TCP_ADDRESS + generateRandomPort());
    }
}
