package com.limechain.storage;

import com.limechain.config.HostConfig;
import lombok.extern.java.Log;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@Log
public class RocksDBInitializer {
    public static final String defaultDirectory = "./rocks-db";
    public static final String testDirectory = "./test-rocks-db";
    private static Map<String, RocksDB> pathToInstances = new HashMap<>();

    public static RocksDB initialize(String path) {
        if (pathToInstances.containsKey(path)) {
            return pathToInstances.get(path);
        }

        RocksDB.loadLibrary();
        try (final Options options = new Options().setCreateIfMissing(true)) {
            RocksDB db = RocksDB.open(options, path);
            pathToInstances.put(path, db);
            return db;
        } catch (RocksDBException e) {
            log.log(Level.SEVERE, "RocksDB initialize exception caught!", e);
            return null;
        }
    }

    public static RocksDB initialize(HostConfig hostConfig) {
        return initialize(hostConfig.getRocksDbPath());
    }

    public static RocksDB initializeTestDatabase() {
        return initialize(testDirectory);
    }

    public static void closeInstances() {
        for (Map.Entry<String, RocksDB> set : pathToInstances.entrySet()) {
            set.getValue().close();
        }
        pathToInstances = new HashMap<>();
    }
}
