package com.gaumji.util;

import io.qameta.allure.Allure;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

public class AllureEnvironmentWriter {

    // ✅ Use ThreadLocal for thread-specific storage and synchronized global set for all browsers
    private static final ThreadLocal<Set<String>> threadBrowsers = ThreadLocal.withInitial(HashSet::new);
    private static final Set<String> allBrowsers = Collections.synchronizedSet(new HashSet<>());

    private static final String os = System.getProperty("os.name");
    private static final String osVersion = System.getProperty("os.version");
    private static final String osArch = System.getProperty("os.arch");
    private static final String javaVersion = System.getProperty("java.version");

    public static void addBrowserInfo(RemoteWebDriver driver) {
        if (driver == null) return;
        Capabilities caps = driver.getCapabilities();
        String browser = caps.getBrowserName() + " " + caps.getBrowserVersion();
        threadBrowsers.get().add(browser);
        allBrowsers.add(browser);
        System.out.println("AllureEnvironmentWriter: Adding browser info for thread '" + Thread.currentThread().getName() + "': " + browser);
    }

    public static void writeEnvironmentInfo() {
        Set<String> browsers = threadBrowsers.get();
        if (browsers == null || browsers.isEmpty()) {
            System.out.println("⚠️ No browser info available for thread: " + Thread.currentThread().getName());
            return;
        }

        Properties props = new Properties();
        int count = 1;
        for (String browser : allBrowsers) { // Use global set to include all browsers
            props.setProperty("Browser." + count, browser);
            count++;
        }

        // Add common properties
        props.setProperty("Selenium.Grid", "true");
        props.setProperty("Execution.Mode", "Grid");
        props.setProperty("OS", os);
        props.setProperty("OS.Version", osVersion);
        props.setProperty("OS.Arch", osArch);
        props.setProperty("Java.Version", javaVersion);
        props.setProperty("Environment", System.getProperty("env", "qa"));
        props.setProperty("Test.Suite", System.getProperty("TEST_SUITE", "regression.xml"));
        props.setProperty("Thread.Count", System.getProperty("THREAD_COUNT", "2"));

        try {
            String browserName = System.getProperty("browser", "unknown").toLowerCase();
            String outputDir = "target/allure-results";
            if (browserName.contains("chrome")) {
                outputDir = "target/allure-results-chrome";
            } else if (browserName.contains("firefox")) {
                outputDir = "target/allure-results-firefox";
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