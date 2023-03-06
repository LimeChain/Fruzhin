package com.limechain.storage;

import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

public class RocksDBTable {
    private final RocksDB db;
    private final String prefix;
    private final byte[] prefixBytes;

    public RocksDBTable (RocksDB db, String prefix) {
        this.db = db;
        this.prefix = prefix;
        this.prefixBytes = prefix.getBytes();
    }

    private byte[] prependPrefixToKey (byte[] key) {
        byte[] prefixedKey = new byte[key.length + this.prefix.length()];

        System.arraycopy(prefixBytes, 0, prefixedKey, 0, prefixBytes.length);

        System.arraycopy(key, prefixBytes.length - prefixBytes.length, prefixedKey, prefixBytes.length, key.length + prefixBytes.length - prefixBytes.length);

        return prefixedKey;

    }

    public void put (byte[] key, byte[] value) {
        byte[] prefixedKey = this.prependPrefixToKey(key);
        try {
            this.db.put(prefixedKey, value);
        } catch (RocksDBException e) {
            System.out.println(e.getMessage());
        }
    }

    public boolean has (byte[] key) {
        byte[] value = this.get(key);
        return value != null;
    }

    public byte[] get (byte[] key) {
        byte[] prefixedKey = this.prependPrefixToKey(key);
        try {
            return this.db.get(prefixedKey);
        } catch (RocksDBException e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    public void del (byte[] key) throws RocksDBException {
        byte[] prefixedKey = this.prependPrefixToKey(key);
        this.db.delete(prefixedKey);
    }

    public void close () {
        this.db.close();
    }
}
