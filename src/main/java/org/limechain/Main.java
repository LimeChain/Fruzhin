package org.limechain;

import org.limechain.lightClient.LightClient;
import org.limechain.rpc.RPC;
import org.limechain.storage.RocksDBInitializer;
import org.rocksdb.RocksDB;

public class Main {
    public static void main (String[] args) {
        RocksDB db = RocksDBInitializer.initialize();
        RPC rpc = new RPC();
        LightClient client = new LightClient(rpc);
        client.start();
    }
}