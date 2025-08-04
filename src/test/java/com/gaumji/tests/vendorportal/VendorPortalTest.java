package com.gaumji.tests.vendorportal;

import com.gaumji.pages.vendorportal.DashboardPage;
import com.gaumji.pages.vendorportal.LoginPage;
import com.gaumji.tests.AbstractTest;
import com.gaumji.tests.vendorportal.model.VendorPortalTestData;
import com.gaumji.util.Config;
import com.gaumji.util.Constants;
import com.gaumji.util.JsonUtil;
import com.gaumji.util.ResourceLoader;
import io.qameta.allure.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.*;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Epic("Vendor Portal")
@Feature("Vendor Dashboard and Logout Flow")
@Listeners({com.gaumji.listener.TestListener.class})
public class VendorPortalTest extends AbstractTest {

    private static final Logger log = LoggerFactory.getLogger(VendorPortalTest.class);

    private LoginPage loginPage;
    private DashboardPage dashboardPage;
    private VendorPortalTestData testData;

    @BeforeTest
    @Parameters("testDataPath")
    public void setPageObjects(String testDataPath) {
        log.info("Initializing page objects and loading test data from: {}", testDataPath);

        this.loginPage = new LoginPage(driver);
        this.dashboardPage = new DashboardPage(driver);

        // ✅ Attach JSON to Allure
        try (InputStream is = ResourceLoader.getResource(testDataPath)) {
            String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            attachTestData(json);
            this.testData = JsonUtil.fromJson(json, VendorPortalTestData.class);
        } catch (Exception e) {
            log.error("Error loading test data", e);
            throw new RuntimeException(e);
        }
    }

    @Attachment(value = "Test Data", type = "application/json")
    public String attachTestData(String json) {
        return json;
    }

    @Test
    @Story("Login Functionality")
    @Severity(SeverityLevel.BLOCKER)
    @Description("Login to the Vendor Portal using provided credentials.")
    public void loginTest() {
        // ✅ FIX: Add this line to set the browser parameter for the Allure report
        setBrowserAsAllureParameter();
        log.info("Navigating to Vendor Portal URL: {}", Config.get(Constants.VENDOR_PORTAL_URL));
        loginPage.goTo(Config.get(Constants.VENDOR_PORTAL_URL));
        Assert.assertTrue(loginPage.isAt(), "Login page should be loaded.");

        log.info("Performing login with user: {}", testData.username());
        loginPage.login(testData.username(), testData.password());
    }

    @Test(dependsOnMethods = "loginTest")
    @Story("Dashboard Verification")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Verify all dashboard metrics and order history search.")
    public void dashboardTest() {
        Assert.assertTrue(dashboardPage.isAt(), "Dashboard should be loaded.");
        log.info("Verifying dashboard metrics...");

        Assert.assertEquals(dashboardPage.getMonthlyEarning(), testData.monthlyEarning(), "Monthly earnings mismatch");
        Assert.assertEquals(dashboardPage.getAnnualEarning(), testData.annualEarning(), "Annual earnings mismatch");
        Assert.assertEquals(dashboardPage.getProfitMargin(), testData.profitMargin(), "Profit margin mismatch");
        Assert.assertEquals(dashboardPage.getAvailableInventory(), testData.availableInventory(), "Inventory mismatch");

        log.info("Performing order search with keyword: {}", testData.searchKeyword());
        dashboardPage.searchOrderHistory(testData.searchKeyword());

        Assert.assertEquals(dashboardPage.getResultsCount(), testData.searchResultsCount(), "Search results count mismatch");
    }

    @Test(dependsOnMethods = "dashboardTest")
    @Story("Logout Functionality")
    @Severity(SeverityLevel.NORMAL)
    @Description("Logout from the Vendor Portal and verify redirection to Login Page.")
    public void logoutTest() {
        log.info("Initiating logout flow...");
        dashboardPage.logout();
        Assert.assertTrue(this.loginPage.isAt(), "User should be redirected to login page after logout.");
    }
}
