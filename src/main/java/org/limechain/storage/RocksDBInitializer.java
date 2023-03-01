package org.limechain.storage;

import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

public class RocksDBInitializer {

    public static String testDirectory = "./test-rocks-db";
    public static String defaultDirectory = "./rocks-db";

    public static RocksDB initialize(String path){
        RocksDB.loadLibrary();

        try (final Options options = new Options().setCreateIfMissing(true)){
            final RocksDB db = RocksDB.open(options, path);
            return db;

        } catch (RocksDBException e){
            System.out.println(e.getMessage());
            return null;
        }
    }

    public static RocksDB initialize(){
        return initialize(defaultDirectory);
    }

    public static RocksDB initializeTestDatabase(){
        return initialize(testDirectory);
    }
}
