package com.limechain.storage.crypto;

import com.limechain.chain.Chain;
import com.limechain.storage.DBInitializer;
import com.limechain.storage.DBRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class KeyStoreTest {

    private DBRepository dbRepository;

    private KeyStore keyStore;

    @BeforeEach
    public void setup() {
        dbRepository = DBInitializer.initialize("./test", Chain.WESTEND, false);
        keyStore = new KeyStore(dbRepository);
    }

    byte[] key = {1,2,3};
    byte[] value = {1,2,3, 4};
    @Test
    void saveAndGetKey() {
        keyStore.put(KeyType.BABE, key, value);
        keyStore.put(KeyType.GRANDPA, key, value);
        List<byte[]> publicKeysByKeyType = keyStore.getPublicKeysByKeyType(KeyType.BABE);
        assertEquals(publicKeysByKeyType.get(0).length, key.length);
        byte[] bytes = keyStore.get(KeyType.BABE, key);
        assertArrayEquals(bytes, value);
    }

}
