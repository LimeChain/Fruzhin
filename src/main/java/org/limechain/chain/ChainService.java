package org.limechain.chain;

import org.limechain.config.HostConfig;
import org.limechain.rpc.RPCContext;
import org.limechain.storage.RocksDBTable;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

import java.io.*;

public class ChainService {
    public ChainSpec genesis;

    public ChainService (HostConfig hostConfig) {
        try {
            RocksDB db = RPCContext.getBean(RocksDB.class);
            RocksDBTable configTable = new RocksDBTable(db, "config");
            try {
                // Loading chain spec from database
                if (configTable.has("genesis".getBytes())) {
                    byte[] genesisBytes = configTable.get("genesis".getBytes());
                    ByteArrayInputStream genesisBytesStream = new ByteArrayInputStream(genesisBytes);
                    ObjectInputStream genesisObjectStream = new ObjectInputStream(genesisBytesStream);
                    this.genesis = (ChainSpec) genesisObjectStream.readObject();
                    System.out.println("✅️Loaded chain spec from database");
                } else {
                    throw new IllegalStateException("No chain spec data in database");
                }
            } catch (ClassNotFoundException | IllegalStateException | IOException e) {
                System.out.println("Error loading chain spec from database. Loading from json...");
                this.genesis = ChainSpec.NewFromJSON(hostConfig.genesisPath);
                System.out.println("✅️Loaded chain spec");

                //Save chain spec to database
                try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
                     ObjectOutputStream oos = new ObjectOutputStream(bos)) {
                    oos.writeObject(this.genesis);
                    configTable.put("genesis".getBytes(), bos.toByteArray());
                    System.out.println("Saved chain spec to database");
                } catch (RocksDBException | IOException saveError){
                    System.out.println("Warning: Could not save chain spec to database");
                }
            }
        } catch (IOException e) {
            System.out.println("Failed to load chain spec");
            System.exit(1);
        }
    }
}
