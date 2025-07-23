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
        properties = loadProperties();

        //check for any override
//        for (String key : properties.stringPropertyNames()) {
//            if (System.getProperties().contains(key)) {
//                properties.setProperty(key, System.getProperty(key));
//            }
//        }

        //commented above for loop(suggested by vinoth, it dint work) so used the below by chatgpt
        for (String key : properties.stringPropertyNames()) {
            String systemProperty = System.getProperty(key);
            if (systemProperty != null) {  // Correctly checking if the system property is set
                properties.setProperty(key, systemProperty);
            }
        }

        //print in the console for debugging purpose
        log.info("Test Properties");
        log.info("------------------------------");
        for (String key : properties.stringPropertyNames()) {
            log.info("{}={}", key, properties.getProperty(key));
        }
        log.info("------------------------------");

    }

    public static String get(String key) {
        return properties.getProperty(key);
    }

    private static Properties loadProperties() {
        Properties properties = new Properties();
        try (InputStream stream = ResourceLoader.getResource(DEFAULT_PROPERTIES)) {
            properties.load(stream);
        } catch (Exception e) {
            log.error("Unable to read property file {}", DEFAULT_PROPERTIES, e);
        }
        return properties;
    }

}
