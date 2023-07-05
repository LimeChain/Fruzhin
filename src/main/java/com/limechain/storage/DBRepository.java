package com.limechain.storage;

import lombok.extern.java.Log;
import org.apache.commons.io.FileUtils;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.springframework.util.SerializationUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.logging.Level;

/**
 * Implementation for Key-Value DB interface with String as key and Object as value types
 */
@Log
public class DBRepository implements KVRepository<String, Object> {
    /**
     * Main DB folder
     */
    private final static String FOLDER_NAME = "db";

    /**
     * Connection to the DB
     */
    private RocksDB db;
    private String chainPrefix;

    public DBRepository(String path, String chain, boolean dbRecreate) {
        RocksDB.loadLibrary();
        final Options options = new Options();
        options.setCreateIfMissing(true);
        File baseDir = Path.of(path, FOLDER_NAME, chain).toFile();
        if (dbRecreate) {
            cleanDatabaseFolder(baseDir);
        }
        chainPrefix = chain;
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

    private void cleanDatabaseFolder(File file) {
        try {
            if (file.exists()) {
                FileUtils.cleanDirectory(file.getAbsoluteFile());
                log.log(Level.INFO, "\uD83D\uDDD1️DB cleaned");
            }

        } catch (IOException e) {
            log.log(Level.SEVERE, String.format("Error deleting db folder. Exception: '%s', message: '%s'",
                            e.getCause(),
                            e.getMessage()),
                    e);
            throw new RuntimeException();
        }
    }

    @Override
    public synchronized boolean save(String key, Object value) {
        log.log(Level.INFO, String.format("saving value '%s' with key '%s'", value, key));
        try {
            db.put(getPrefixedKey(key), SerializationUtils.serialize(value));
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
            byte[] bytes = db.get(getPrefixedKey(key));
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
            db.delete(getPrefixedKey(key));
        } catch (RocksDBException e) {
            log.log(Level.SEVERE,
                    String.format("Error deleting entry, cause: '%s', message: '%s'", e.getCause(), e.getMessage()));
            return false;
        }
        return true;
    }

    public byte[] getPrefixedKey(String key) {
        return chainPrefix.concat(key).getBytes();
    }

    public synchronized void closeConnection() {
        this.db.close();
    }

}
