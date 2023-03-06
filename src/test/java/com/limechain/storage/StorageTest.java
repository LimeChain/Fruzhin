package com.limechain.storage;

import org.limechain.storage.RocksDBInitializer;
import org.rocksdb.RocksDB;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class StorageTest {

    @Test
    public void initializeStorageTest(){
        RocksDB db = RocksDBInitializer.initializeTestDatabase();
        assertNotNull(db);
        db.close();
    }

}
