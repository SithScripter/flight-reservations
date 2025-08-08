package com.gaumji.util;

import io.qameta.allure.Allure;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

public class AllureEnvironmentWriter {

    public static void writeEnvironmentInfo(RemoteWebDriver driver) {
        Properties props = new Properties();

        // --- Get Browser and OS details from the live driver session ---
        if (driver != null) {
            Capabilities caps = driver.getCapabilities();
            String browserName = caps.getBrowserName();
            String browserVersion = caps.getBrowserVersion();

            props.setProperty("Browser", browserName);
            props.setProperty("Browser.Version", browserVersion);
            props.setProperty("OS", caps.getPlatformName().toString());

            // Add a label to each test case for easy filtering in the Allure report
            Allure.label("browser", browserName);
        }

        // --- Get Test Execution details from system properties ---
        props.setProperty("Java.Version", System.getProperty("java.version"));
        props.setProperty("Selenium.Grid", Config.get(Constants.GRID_ENABLED));
        props.setProperty("Environment", System.getProperty("env", "N/A"));
        props.setProperty("Test.Suite", System.getProperty("TEST_SUITE", "N/A"));
        props.setProperty("Thread.Count", System.getProperty("THREAD_COUNT", "N/A"));

        // --- Write the properties to the allure-results directory ---
        try {
            File allureResultsDir = new File("target/allure-results");
            if (!allureResultsDir.exists()) {
                allureResultsDir.mkdirs();
            }
            File environmentFile = new File(allureResultsDir, "environment.properties");
            props.store(new FileWriter(environmentFile), "Allure Environment Details");

            System.out.println("✅ Allure environment properties successfully written to: " + environmentFile.getAbsolutePath());

        } catch (IOException e) {
            System.err.println("❌ Error writing Allure environment properties file");
            e.printStackTrace();
        }
    }
}