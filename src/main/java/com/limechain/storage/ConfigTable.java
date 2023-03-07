package com.limechain.storage;

import com.limechain.chain.ChainSpec;
import org.rocksdb.RocksDB;

import java.io.*;

public class ConfigTable extends RocksDBTable {
    public ConfigTable(RocksDB db) {
        super(db, "config");
    }

    public void putGenesis(ChainSpec genesis) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(genesis);
            put("genesis".getBytes(), bos.toByteArray());
        } catch (IOException saveError) {
            System.out.println("Warning: Could not save chain spec to database");
        }
    }

    public ChainSpec getGenesis() throws IllegalStateException {
        if (has("genesis".getBytes())) {
            byte[] genesisBytes = get("genesis".getBytes());
            try (ByteArrayInputStream genesisBytesStream = new ByteArrayInputStream(genesisBytes);
                 ObjectInputStream genesisObjectStream = new ObjectInputStream(genesisBytesStream)) {
                return (ChainSpec) genesisObjectStream.readObject();
            } catch (IOException | ClassNotFoundException e) {
                throw new IllegalStateException("Error loading chain spec from database");
            }
        } else {
            throw new IllegalStateException("No chain spec data in database");
        }
    }
}
