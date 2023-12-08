package com.limechain.storage;

import com.limechain.chain.Chain;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.java.Log;

import java.util.HashMap;
import java.util.Map;

/**
 * Initializer class ala Singleton which holds and serves connections to the database.
 */
@Log
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DBInitializer {
    /**
     * Default directory where the db will be set
     */
    public static final String DEFAULT_DIRECTORY = "./rocks-db";

    /**
     * Hold all instances/connections to the DB in case there's a reason that more than 1 connection is opened
     */
    private static final Map<String, DBRepository> INSTANCES = new HashMap<>();

    /**
     * Initializes the connection if it doesn't exist and returns it
     *
     * @param path    path where the DB should write to
     * @param chain   current network used for prefix
     * @param dbRecreate flag for recreating the database for current chain
     * @return connection to the DB
     */
    public static DBRepository initialize(String path, Chain chain, boolean dbRecreate) {
        if (INSTANCES.containsKey(path)) {
            return INSTANCES.get(path);
        }

        DBRepository repo = new DBRepository(path, chain.getValue(), dbRecreate);

        INSTANCES.put(path, repo);
        return repo;
    }

    /**
     * Closes all open connections to the DB
     */
    public static void closeInstances() {
        for (Map.Entry<String, DBRepository> set : INSTANCES.entrySet()) {
            set.getValue().closeConnection();
        }
        INSTANCES.clear();
    }
}
