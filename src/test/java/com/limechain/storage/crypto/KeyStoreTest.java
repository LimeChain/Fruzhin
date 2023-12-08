package com.limechain.storage.crypto;

import com.limechain.chain.Chain;
import com.limechain.storage.DBInitializer;
import com.limechain.storage.DBRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class KeyStoreTest {

    byte[] key = {1, 2, 3};
    byte[] key2 = {4, 5, 6};
    byte[] key3 = {7, 8, 9};
    byte[] value = {1, 2, 3, 4};
    byte[] value2 = {5, 6, 7, 8};
    byte[] value3 = {9, 10, 11, 12};
    private DBRepository dbRepository;
    private KeyStore keyStore;

    @BeforeEach
    public void setup() {
        dbRepository = DBInitializer.initialize("./test", Chain.WESTEND, true);
        keyStore = new KeyStore(dbRepository);
    }

    @Test
    void saveAndGetKey() {
        System.out.println(keyStore);
        keyStore.put(KeyType.BABE, key, value);
        keyStore.put(KeyType.GRANDPA, key2, value2);
        keyStore.put(KeyType.BABE, key3, value3);

        byte[] privKey = keyStore.get(KeyType.BABE, key);
        byte[] privKey2 = keyStore.get(KeyType.GRANDPA, key2);
        byte[] privKey3 = keyStore.get(KeyType.BABE, key3);

        assertArrayEquals(privKey, value);
        assertArrayEquals(privKey2, value2);
        assertArrayEquals(privKey3, value3);

        byte[] invPrivKey = keyStore.get(KeyType.GRANDPA, key);
        byte[] invPrivKey2 = keyStore.get(KeyType.BABE, key2);
        byte[] invPrivKey3 = keyStore.get(KeyType.GRANDPA, key3);

        assertNull(invPrivKey);
        assertNull(invPrivKey2);
        assertNull(invPrivKey3);
    }

    @Test
    void saveAndGetCommonKey() {
        keyStore.put(KeyType.BABE, key, value);
        keyStore.put(KeyType.GRANDPA, key2, value2);
        keyStore.put(KeyType.BABE, key3, value3);

        List<byte[]> publicKeysByKeyType = keyStore.getPublicKeysByKeyType(KeyType.BABE);

        assertEquals(2, publicKeysByKeyType.size());

        List<byte[]> publicKeysByKeyTypeGrandpa = keyStore.getPublicKeysByKeyType(KeyType.GRANDPA);
        assertEquals(publicKeysByKeyTypeGrandpa.get(0).length, key2.length);
    }

}
