package com.gaumji.util;

import io.qameta.allure.Allure;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

public class AllureEnvironmentWriter {

    // ✅ FIX: Use ThreadLocal to provide separate, safe storage for each parallel thread.
    private static final ThreadLocal<Set<String>> threadBrowsers = ThreadLocal.withInitial(HashSet::new);
    private static final String os = System.getProperty("os.name");
    private static final String javaVersion = System.getProperty("java.version");

    // This method is called by each thread to safely store its own browser information.
    public static void addBrowserInfo(RemoteWebDriver driver) {
        if (driver == null) return;
        Capabilities caps = driver.getCapabilities();
        String browser = caps.getBrowserName() + " " + caps.getBrowserVersion();
        threadBrowsers.get().add(browser);
        // This debug line confirms which browser is being added by which thread.
        System.out.println("AllureEnvironmentWriter: Adding browser info for thread '" + Thread.currentThread().getName() + "': " + browser);
    }

    // This is called at the end of each test to write the thread-specific environment file.
    public static void writeEnvironmentInfo() {
        Set<String> browsers = threadBrowsers.get();
        if (browsers == null || browsers.isEmpty()) {
            return;
        }

        Properties props = new Properties();
        int count = 1;
        for (String browser : browsers) {
            props.setProperty("Browser." + count, browser);
            count++;
        }

        props.setProperty("Selenium.Grid", "true");
        props.setProperty("Execution.Mode", "Grid");
        props.setProperty("OS", os);
        props.setProperty("Java.Version", javaVersion);

        try {
            // ✅ Extract browser name to determine output folder
            String outputDir = "target/allure-results"; // fallback
            for (String browser : browsers) {
                if (browser.toLowerCase().contains("chrome")) {
                    outputDir = "target/allure-results-chrome";
                    break;
                } else if (browser.toLowerCase().contains("firefox")) {
                    outputDir = "target/allure-results-firefox";
                    break;
                }
            }

            File file = new File(outputDir, "environment.properties");
            file.getParentFile().mkdirs();
            props.store(new FileWriter(file), "Allure environment details");

            System.out.println("✅ Environment info written to: " + file.getAbsolutePath());

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            threadBrowsers.remove();
        }
    }

    public static void addBrowserLabel(RemoteWebDriver driver) {
        if (driver != null) {
            Capabilities caps = driver.getCapabilities();
            Allure.label("browser", caps.getBrowserName());
            Allure.label("browserVersion", caps.getBrowserVersion());
        }
    }
}