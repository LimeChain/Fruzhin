package com.limechain.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class Config {
    private static final Logger LOGGER = Logger.getLogger(Config.class.getName());
    private static final String CONFIG_FILE_NAME = "app.config";

    protected Properties readConfig () {
        Properties properties = null;
        try (FileInputStream fis = new FileInputStream(CONFIG_FILE_NAME)) {
            properties = new Properties();
            properties.load(fis);
        } catch (IOException ex) {
            LOGGER.log(Level.WARNING, String.format("Failed to find the config file(%s)%n", CONFIG_FILE_NAME));
            System.exit(1);
        }
        return properties;
    }

}
