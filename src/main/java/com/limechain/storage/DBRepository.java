package com.limechain.storage;

import lombok.extern.java.Log;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.springframework.util.SerializationUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;
import java.util.logging.Level;

@Log
public class DBRepository implements KVRepository<String, Object> {
    private final static String FILE_NAME = "db";
    private RocksDB db;

    public DBRepository(String path) {
        RocksDB.loadLibrary();
        final Options options = new Options();
        options.setCreateIfMissing(true);
        File baseDir = new File(path, FILE_NAME);
        try {
            Files.createDirectories(baseDir.getParentFile().toPath());
            Files.createDirectories(baseDir.getAbsoluteFile().toPath());
            db = RocksDB.open(options, baseDir.getAbsolutePath());
            log.log(Level.INFO, "\uD83E\uDEA8RocksDB initialized");
        } catch (IOException | RocksDBException e) {
            log.log(Level.SEVERE, String.format("Error initializing RocksDB. Exception: '%s', message: '%s'",
                            e.getCause(),
                            e.getMessage()),
                    e);
        }
    }

    @Override
    public synchronized boolean save(String key, Object value) {
        log.log(Level.INFO, String.format("saving value '%s' with key '%s'", value, key));
        try {
            db.put(key.getBytes(), SerializationUtils.serialize(value));
        } catch (RocksDBException e) {
            log.log(Level.WARNING,
                    String.format("Error saving entry. Cause: '%s', message: '%s'", e.getCause(), e.getMessage()));
            return false;
        }
        return true;
    }

    @Override
    public synchronized Optional<Object> find(String key) {
        Object value = null;
        try {
            byte[] bytes = db.get(key.getBytes());
            if (bytes != null) {
                value = SerializationUtils.deserialize(bytes);
            }
        } catch (RocksDBException e) {
            log.log(Level.SEVERE, String.format(
                    "Error retrieving the entry with key: %s, cause: %s, message: %s",
                    key,
                    e.getCause(),
                    e.getMessage())
            );
        }
        log.log(Level.INFO, String.format("finding key '%s' returns '%s'", key, value));
        return value != null ? Optional.of(value) : Optional.empty();
    }

    @Override
    public synchronized boolean delete(String key) {
        log.log(Level.INFO, String.format("deleting key '%s'", key));
        try {
            db.delete(key.getBytes());
        } catch (RocksDBException e) {
            log.log(Level.SEVERE,
                    String.format("Error deleting entry, cause: '%s', message: '%s'", e.getCause(), e.getMessage()));
            return false;
        }
        return true;
    }

    public synchronized void closeConnection() {
        this.db.close();
    }

}