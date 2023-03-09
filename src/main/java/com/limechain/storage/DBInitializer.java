package com.limechain.storage;

import lombok.extern.java.Log;

import java.util.HashMap;
import java.util.Map;

@Log
public class DBInitializer {
    public static final String DEFAULT_DIRECTORY = "./rocks-db";
    private static Map<String, DBRepository> instances = new HashMap<>();

    public static DBRepository initialize(String path) {
        if (instances.containsKey(path)) {
            return instances.get(path);
        }

        DBRepository repo = new DBRepository(path);

        instances.put(path, repo);
        return repo;
    }

    public static void closeInstances() {
        for (Map.Entry<String, DBRepository> set : instances.entrySet()) {
            set.getValue().closeConnection();
        }
        instances = new HashMap<>();
    }
}
