package com.limechain.storage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rocksdb.RocksDB;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class StorageTest {
    private RocksDB db;

    @BeforeEach
    public void setup() {
        db = RocksDBInitializer.initializeTestDatabase();
    }

    @AfterEach
    public void close() {
        RocksDBInitializer.closeInstances();
    }

    @Test
    public void initializeStorageTest() {
        assertNotNull(db);
        db.close();
    }

}
