package com.gaumji.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Properties;

public class Config {

    private static final Logger log = LoggerFactory.getLogger(Config.class);
    private static final String DEFAULT_PROPERTIES = "config/default.properties";
    private static Properties properties;

    public static void initialize() {
        // load default properties
        // ✅ CHANGE: The loadProperties method now requires a path argument
        properties = loadProperties(DEFAULT_PROPERTIES);

        // ✅ ADDITION: Start of new logic to load environment-specific properties
        String environment = System.getProperty("env"); // Reads the -Denv=qa property
        if (environment != null && !environment.trim().isEmpty()) {
            log.info("Loading properties for environment: {}", environment);
            String envPropertiesPath = "config/" + environment.toLowerCase() + ".properties";
            Properties envProperties = loadProperties(envPropertiesPath);
            // This will override any default properties with the ones from the environment file
            properties.putAll(envProperties);
        }
        // ✅ ADDITION: End of new logic

        // This original block for overriding with any other system properties remains the same
        for (String key : properties.stringPropertyNames()) {
            String systemProperty = System.getProperty(key);
            if (systemProperty != null) {  // Correctly checking if the system property is set
                properties.setProperty(key, systemProperty);
            }
        }

        //print in the console for debugging purpose
        log.info("--- Final Test Properties ---");
        for (String key : properties.stringPropertyNames()) {
            log.info("{}={}", key, properties.getProperty(key));
        }
        log.info("-----------------------------");
    }

    public static String get(String key) {
        return properties.getProperty(key);
    }

    // ✅ CHANGE: The method is now generic and accepts a 'path' so it can load any properties file.
    private static Properties loadProperties(String path) {
        Properties properties = new Properties();
        try (InputStream stream = ResourceLoader.getResource(path)) {
            properties.load(stream);
        } catch (Exception e) {
            log.error("Unable to read property file {}", path, e);
        }
        return properties;
    }
}