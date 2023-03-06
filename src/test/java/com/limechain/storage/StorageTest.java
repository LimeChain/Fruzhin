package com.limechain.storage;

import org.junit.jupiter.api.Test;
import org.rocksdb.RocksDB;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class StorageTest {

    @Test
    public void initializeStorageTest () {
        RocksDB db = RocksDBInitializer.initializeTestDatabase();
        assertNotNull(db);
        db.close();
    }

}
