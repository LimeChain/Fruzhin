package org.limechain.storage;

import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

public class RocksDBTable {
    RocksDB db;
    String prefix;
    byte[] prefixBytes;

    public RocksDBTable(RocksDB db, String prefix) {
        this.db = db;
        this.prefix = prefix;
        this.prefixBytes = prefix.getBytes();
    }

    private byte[] prependPrefixToKey(byte[] key) {
        byte[] prefixedKey = new byte[key.length + this.prefix.length()];

        for (int i = 0; i < prefixBytes.length; i++) {
            prefixedKey[i] = prefixBytes[i];
        }

        for (int i = prefixBytes.length; i < (key.length + prefixBytes.length); i++)
            prefixedKey[i] = key[i - prefixBytes.length];

        return prefixedKey;

    }

    public void put(byte[] key, byte[] value) throws RocksDBException {
            byte[] prefixedKey = this.prependPrefixToKey(key);
            try {
                this.db.put(prefixedKey, value);
            } catch (RocksDBException e){
                System.out.println(e.getMessage());
            }
    };

    public boolean has(byte[] key) throws RocksDBException {
        byte[] value = this.get(key);
        if (value != null) {
            return true;
        }
        return false;
    }

    public byte[] get(byte[] key) {
        byte[] prefixedKey = this.prependPrefixToKey(key);
        try {
            return this.db.get(prefixedKey);
        } catch (RocksDBException e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    public void del(byte[] key) throws RocksDBException {
        byte[] prefixedKey = this.prependPrefixToKey(key);
        this.db.delete(prefixedKey);
    }

    public void close() {
        this.db.close();
    }
}
