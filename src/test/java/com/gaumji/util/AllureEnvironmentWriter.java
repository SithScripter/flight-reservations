package com.gaumji.util;

import io.qameta.allure.Allure;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;

public class AllureEnvironmentWriter {

    private static final Set<String> usedBrowsers = new LinkedHashSet<>();
    private static String os = System.getProperty("os.name");
    private static String javaVersion = System.getProperty("java.version");
    private static boolean gridEnabled = Boolean.getBoolean("selenium.grid.enabled");

    // Collect browser info (instead of writing immediately)
    public static void writeEnvironmentInfo(RemoteWebDriver driver) {
        Capabilities caps = driver.getCapabilities();
        String browser = caps.getBrowserName() + " " + caps.getBrowserVersion();
        usedBrowsers.add(browser);
    }

    // Called once after all tests finish
    public static void writeEnvironmentInfo() {
        Properties props = new Properties();

        int count = 1;
        for (String browser : usedBrowsers) {
            props.setProperty("Browser." + count, browser);
            count++;
        }

        props.setProperty("Selenium.Grid", String.valueOf(gridEnabled));
        props.setProperty("Execution.Mode", gridEnabled ? "Grid" : "Local");
        props.setProperty("OS", os);
        props.setProperty("Java.Version", javaVersion);

        try {
            File file = new File("target/allure-results/environment.properties");
            file.getParentFile().mkdirs();
            props.store(new FileWriter(file), "Allure environment details");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ✅ Per-test tagging in Allure
    public static void addBrowserLabel(RemoteWebDriver driver) {
        if (driver != null) {
            Capabilities caps = driver.getCapabilities();
            Allure.label("browser", caps.getBrowserName());
            Allure.label("browserVersion", caps.getBrowserVersion());
        }
    }
}
