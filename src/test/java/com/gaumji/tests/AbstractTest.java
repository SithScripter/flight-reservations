package com.gaumji.tests;

import com.gaumji.listener.TestListener;
import com.gaumji.util.Config;
import com.gaumji.util.Constants;
import com.google.common.util.concurrent.Uninterruptibles;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.ITestContext;
import org.testng.annotations.*;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.time.Duration;

@Listeners({TestListener.class})
public abstract class AbstractTest {
    protected WebDriver driver;

    private static final Logger log = LoggerFactory.getLogger(AbstractTest.class);

    @BeforeSuite
    public void setUpConfiguration(){
        Config.initialize();
    }

    @BeforeTest
    public void setDriver(ITestContext ctx) throws MalformedURLException {
//        if(Boolean.parseBoolean(Config.get(Constants.GRID_ENABLED))){
//            if(Boolean.getBoolean("selenium.grid.enabled")){
//                this.driver = getRemoteDriver();
//            }else{
//                this.driver = getLocalDriver();
//            }
//        }
        //instead of using above lengthy process, we can use ternary operator
        this.driver = Boolean.parseBoolean(Config.get(Constants.GRID_ENABLED)) ? getRemoteDriver() : getLocalDriver();
        ctx.setAttribute(Constants.DRIVER, this.driver);
    }

    private WebDriver getRemoteDriver() throws MalformedURLException {
        Capabilities capabilities = new ChromeOptions();
        if(Constants.FIREFOX.equalsIgnoreCase(Config.get(Constants.BROWSER))){
            capabilities = new FirefoxOptions();
        }
        String urlFormat = Config.get(Constants.GRID_URL_FORMAT);
        String hubHost = Config.get(Constants.GRID_HUB_HOST);
        String url = String.format(urlFormat, hubHost);
        log.info("grid url: {}",url);
//        return new RemoteWebDriver(new URL(url), capabilities); //'URL(java.lang.String)' is deprecated since version 20
        return new RemoteWebDriver(
                URI.create(url).toURL(),  // Correct conversion
                capabilities
        );
    }

    private WebDriver getLocalDriver(){
        this.driver = new ChromeDriver();
        driver.manage().window().setSize(new Dimension(1920, 1080));
        return this.driver;
    }

    @AfterTest
    public void tearDown() {
        this.driver.quit();
    }

//    use below method, if you wish to watch the test execution video VNC in docker vms, else comment this
//    @AfterMethod
    public void sleep(){
        Uninterruptibles.sleepUninterruptibly(Duration.ofSeconds(5));
    }

}
