package com.limechain.storage;

import com.limechain.chain.Chain;
import lombok.extern.java.Log;

import java.util.HashMap;
import java.util.Map;

/**
 * Initializer class ala Singleton which holds and serves connections to the database.
 * <p>
 * Both http and ws Spring apps should hold the same connection to the database.
 * Additionally, RocksDB throws an error if there are two apps trying to write to the same directory
 */
@Log
public class DBInitializer {
    /**
     * Default directory where the db will be set
     */
    public static final String DEFAULT_DIRECTORY = "./rocks-db";

    /**
     * Hold all instances/connections to the DB in case there's a reason that more than 1 connections is opened
     */
    private static Map<String, DBRepository> instances = new HashMap<>();

    /**
     * Initializes the connection if it doesn't exist and returns it
     *
     * @param path path where the DB should write to
     * @return connection to the DB
     */
    public static DBRepository initialize(String path, Chain chain) {
        if (instances.containsKey(path)) {
            return instances.get(path);
        }

        DBRepository repo = new DBRepository(path, chain.getValue());

        instances.put(path, repo);
        return repo;
    }

    /**
     * Closes all open connections to the DB
     */
    public static void closeInstances() {
        for (Map.Entry<String, DBRepository> set : instances.entrySet()) {
            set.getValue().closeConnection();
        }
        instances = new HashMap<>();
    }
}
