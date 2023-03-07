package com.limechain.config;

import lombok.extern.java.Log;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;

@Log
public abstract class Config {
    private static final String CONFIG_FILE_NAME = "app.config";

    protected Properties readConfig () {
        Properties properties;
        try (FileInputStream fis = new FileInputStream(CONFIG_FILE_NAME)) {
            properties = new Properties();
            properties.load(fis);
        } catch (IOException ex) {
            log.log(Level.WARNING, String.format("Failed to find the config file(%s)%n", CONFIG_FILE_NAME));
            throw new RuntimeException();
        }
        return properties;
    }

}
