package com.gaumji.tests;

import com.gaumji.listener.TestListener;
import com.gaumji.util.AllureEnvironmentWriter;
import com.gaumji.util.Config;
import com.gaumji.util.Constants;
import com.google.common.util.concurrent.Uninterruptibles;
import io.github.bonigarcia.wdm.WebDriverManager;
import io.qameta.allure.Allure;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestContext;
import org.testng.annotations.*;

import java.net.MalformedURLException;
import java.net.URI;
import java.time.Duration;

@Listeners({TestListener.class})
public abstract class AbstractTest {
    protected WebDriver driver;

    private static final Logger log = LoggerFactory.getLogger(AbstractTest.class);

    @BeforeSuite
    public void setUpConfiguration() {
        Config.initialize();
    }

    @BeforeTest
    public void setDriver(ITestContext ctx) throws MalformedURLException {
        boolean isRemote = Boolean.parseBoolean(Config.get(Constants.GRID_ENABLED));
        // ✅ FIX: Read the browser from the system property, which is passed by Jenkins.
        String browser = System.getProperty("browser");
        if (browser == null) {
            browser = Config.get(Constants.BROWSER);
        }

        log.info("🔧 Driver mode: {}", isRemote ? "Remote Grid" : "Local");
        log.info("🌐 Browser selected: {}", browser);

        this.driver = isRemote ? getRemoteDriver(browser) : getLocalDriver(browser);
        ctx.setAttribute(Constants.DRIVER, this.driver);

        if (this.driver instanceof RemoteWebDriver remoteDriver) {
            // ✅ FIX: Call the new thread-safe method to add browser info.
            AllureEnvironmentWriter.addBrowserInfo(remoteDriver);
            AllureEnvironmentWriter.addBrowserLabel(remoteDriver);
        }
    }

    protected WebDriver getRemoteDriver(String browser) throws MalformedURLException {
        Capabilities capabilities;
        if (browser.equalsIgnoreCase(Constants.FIREFOX)) {
            capabilities = new FirefoxOptions();
        } else {
            capabilities = new ChromeOptions();
        }
        String urlFormat = Config.get(Constants.GRID_URL_FORMAT);
        String hubHost = Config.get(Constants.GRID_HUB_HOST);
        String url = String.format(urlFormat, hubHost);
        log.info("🔗 Running in remote mode with URL: {}", url);
        log.info("🚀 Launching remote browser: {}", browser);
        return new RemoteWebDriver(URI.create(url).toURL(), capabilities);
    }

    protected WebDriver getLocalDriver(String browser) {
        log.info("💻 Running in local mode. Browser: {}", browser);
        WebDriver localDriver;
        if (browser.equalsIgnoreCase(Constants.FIREFOX)) {
            WebDriverManager.firefoxdriver().setup();
            localDriver = new FirefoxDriver();
        } else {
            WebDriverManager.chromedriver().setup();
            localDriver = new ChromeDriver();
        }
        localDriver.manage().window().setSize(new Dimension(1920, 1080));
        return localDriver;
    }

    public void setBrowserAsAllureParameter(){
        String browser = System.getProperty("browser");
        if(browser != null && !browser.isEmpty()){
            Allure.parameter("Browser", browser);
        }
    }


    @AfterTest
    public void tearDown() {
        // ✅ FIX: Write the thread-specific environment file at the end of the test.
        AllureEnvironmentWriter.writeEnvironmentInfo();
        if (this.driver != null) {
            log.info("🧹 Quitting browser session.");
            this.driver.quit();
        }
    }

    @AfterMethod(enabled = false)
    public void sleep() {
        Uninterruptibles.sleepUninterruptibly(Duration.ofSeconds(5));
    }
}