package com.limechain.runtime.research.hybrid.context;

public interface BasicStorage {
    void put(byte[] key, byte[] value);
    byte[] get(byte[] key);
    void delete(byte[] key);
}
