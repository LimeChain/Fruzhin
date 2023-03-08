package com.limechain.storage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

import static org.junit.jupiter.api.Assertions.*;

public class TableTest {

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
    public void Table() throws RocksDBException {
        byte[] key = new byte[]{1, 1, 1};
        byte[] value = new byte[]{4, 5, 6};

        RocksDBTable table = new RocksDBTable(db, "test");

        table.put(key, value);
        assertTrue(table.has(key));
        assertArrayEquals(table.get(key), value);

        table.del(key);
        assertFalse(table.has(key));
    }
}
