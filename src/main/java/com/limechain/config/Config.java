package com.limechain.config;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

public abstract class Config {
    private static final String CONFIG_FILE_NAME = "app.config";

    protected Properties readConfig () {
        Properties properties = null;
        try (FileInputStream fis = new FileInputStream(CONFIG_FILE_NAME)) {
            properties = new Properties();
            properties.load(fis);
        } catch (
                FileNotFoundException ex) {
            System.out.printf("Failed to find the config file(%s)%n", CONFIG_FILE_NAME);
            System.exit(1);
        } catch (
                IOException ex) {
            System.out.printf("Failed to read the config file(%s)%n", CONFIG_FILE_NAME);
            System.exit(1);
        }
        return properties;
    }

}
