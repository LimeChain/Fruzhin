package com.limechain.utils;

import org.apache.tomcat.util.buf.HexUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class HashUtilsTest {

    public static final String BLAKE2B = "11da6d1f761ddf9bdb4c9d6e5303ebd41f61858d0a5647a1a7bfe089bf921be9";
    public static final String BLAKE2B128 = "11d2df4e979aa105cf552e9544ebd2b5";
    public static final String KECCAK256 = "e8e77626586f73b955364c7b4bbf0bb7f7685ebd40e852b164633a4acbd3244c";
    public static final String KECCAK512 = "504ab73753a47f9cca024d3e57f3714a1b726a77367d526aea69e0c987a36766789" +
            "ee746d33091333627d881bf4352ea6d4560be4acb05e581e1a4bca9e542ba";
    public static final String SHA256 = "df3f619804a92fdb4057192dc43dd748ea778adc52bc498ce80524c014b81119";
    public static final String XX64 = "b4def25cfda6ef3a";
    public static final String XX128 = "26aa394eea5630e07c48ae0c9558cef7";
    public static final String XX256 = "26aa394eea5630e07c48ae0c9558cef714355510e01e85b83bb4d561945dad84";

    @Test
    void hashWithBlake2b() {
        byte[] bytes = HashUtils.hashWithBlake2b(HexUtils.fromHexString("00000000"));

        assertArrayEquals(HexUtils.fromHexString(BLAKE2B), bytes);
    }

    @Test
    void hashWithBlake2b128() {
        byte[] bytes = HashUtils.hashWithBlake2b128(HexUtils.fromHexString("00000000"));

        assertArrayEquals(HexUtils.fromHexString(BLAKE2B128), bytes);
    }

    @Test
    void hashWithBlake2bToLength() {
        byte[] bytes = HashUtils.hashWithBlake2bToLength(HexUtils.fromHexString("00000000"), 16);

        System.out.println(HexUtils.toHexString(bytes));
        assertArrayEquals(HexUtils.fromHexString(BLAKE2B128), bytes);
    }

    @Test
    void hashWithKeccak256() {
        byte[] bytes = HashUtils.hashWithKeccak256(HexUtils.fromHexString("00000000"));

        assertArrayEquals(HexUtils.fromHexString(KECCAK256), bytes);
    }

    @Test
    void hashWithKeccak512() {
        byte[] bytes = HashUtils.hashWithKeccak512(HexUtils.fromHexString("00000000"));

        assertArrayEquals(HexUtils.fromHexString(KECCAK512), bytes);
    }

    @Test
    void hashWithSha256() {
        byte[] bytes = HashUtils.hashWithSha256(HexUtils.fromHexString("00000000"));

        assertArrayEquals(HexUtils.fromHexString(SHA256), bytes);
    }

    @Test
    void hashXx64() {
        byte[] bytes = HashUtils.hashXx64(0, HexUtils.fromHexString("00000000"));

        assertArrayEquals(HexUtils.fromHexString(XX64), bytes);
    }

    @Test
    void hashXx128() {
        byte[] bytes = HashUtils.hashXx128(0, "System".getBytes());

        assertArrayEquals(HexUtils.fromHexString(XX128), bytes);
    }

    @Test
    void hashXx256() {
        byte[] bytes = HashUtils.hashXx256(0, "System".getBytes());

        assertArrayEquals(HexUtils.fromHexString(XX256), bytes);
    }
}