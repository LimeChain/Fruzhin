package com.limechain.storage;

import com.limechain.config.HostConfig;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

import java.util.logging.Level;
import java.util.logging.Logger;

public class RocksDBInitializer {

    private static final Logger LOGGER = Logger.getLogger(RocksDBInitializer.class.getName());
    public static String testDirectory = "./test-rocks-db";
    public static String defaultDirectory = "./rocks-db";
    private static RocksDB db;

    public static RocksDB initialize (String path) {
        RocksDB.loadLibrary();
        if (db != null) {
            return db;
        }

        try (final Options options = new Options().setCreateIfMissing(true)) {
            db = RocksDB.open(options, path);
            return db;

        } catch (RocksDBException e) {
            LOGGER.log(Level.SEVERE, "RocksDB initialize exception caught!", e);
            return null;
        }
    }

    public static RocksDB initialize (HostConfig hostConfig) {
        return initialize(hostConfig.rocksDbPath);
    }

    public static RocksDB initializeTestDatabase () {
        return initialize(testDirectory);
    }
}
