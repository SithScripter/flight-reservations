package com.gaumji.tests.flightreservations;

import com.gaumji.pages.flightreservation.*;
import com.gaumji.tests.AbstractTest;
import com.gaumji.tests.flightreservations.model.FlightReservationTestData;
import com.gaumji.util.Config;
import com.gaumji.util.Constants;
import com.gaumji.util.JsonUtil;
import com.gaumji.util.ResourceLoader;
import io.qameta.allure.*;
import io.qameta.allure.testng.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.annotations.*;

import java.io.InputStream;

@Epic("Flight Reservation Module")
@Feature("E2E Flight Booking Flow")
@Owner("lalit.kumar")
@Tag("regression")
@Tag("grid")
public class FlightReservationTest extends AbstractTest {

    private static final Logger log = LoggerFactory.getLogger(FlightReservationTest.class);
    private FlightReservationTestData testData;

    @BeforeTest
    @Parameters("testDataPath")
    public void setParameters(String testDataPath) {
        log.info("📦 Loading test data from: {}", testDataPath);

        // ✅ Attach JSON to Allure
        attachTestData(testDataPath);

        // ✅ Deserialize into testData object
        this.testData = JsonUtil.getData(testDataPath, FlightReservationTestData.class);

        // ✅ FIX: Read the browser name from Java System Properties
        String browser = System.getProperty("browser");

        // Add the label if the browser property exists
        if (browser != null && !browser.isEmpty()) {
            Allure.parameter("Browser", browser);
            log.info("🧭 Injected Allure browser parameter: {}", browser);
        }
    }

    @Attachment(value = "Test Data", type = "application/json")
    public String attachTestData(String path) {
        try (InputStream is = ResourceLoader.getResource(path)) {
            return new String(is.readAllBytes());
        } catch (Exception e) {
            log.error("❌ Failed to attach test data: {}", path, e);
            return "Failed to attach test data from: " + path;
        }
    }

    @Test(description = "User Registration with valid personal, contact, and login credentials")
    @Severity(SeverityLevel.BLOCKER)
    @Story("User Sign-Up")
    @Description("Register a user and verify that registration succeeds.")
    public void userRegistrationTest() {
        log.info("🛫 Starting user registration test...");
        RegistrationPage registrationPage = new RegistrationPage(driver);
        registrationPage.goTo(Config.get(Constants.FLIGHT_RESERVATION_URL));
        Assert.assertTrue(registrationPage.isAt(), "Registration page did not load.");

        registrationPage.enterUserDetails(testData.firstname(), testData.lastname());
        registrationPage.enterUserCredentials(testData.email(), testData.password());
        registrationPage.enterUserAddress(testData.street(), testData.city(), testData.zip());
        registrationPage.register();
        log.info("✅ User registration completed successfully.");
    }

    @Test(dependsOnMethods = "userRegistrationTest", description = "Registration confirmation screen validation")
    @Severity(SeverityLevel.CRITICAL)
    @Story("Registration Verification")
    @Description("Confirm registration and validate first name")
    public void registrationConfirmationTest() {
        log.info("🔍 Validating registration confirmation...");
        RegistrationConfirmationPage confirmationPage = new RegistrationConfirmationPage(driver);
        Assert.assertTrue(confirmationPage.isAt(), "Registration Confirmation page did not load.");
        Assert.assertEquals(confirmationPage.getFirstName(), testData.firstname(), "First name doesn't match.");
        confirmationPage.goToFlightSearch();
    }

    @Test(dependsOnMethods = "registrationConfirmationTest", description = "Passenger selection and flight search")
    @Severity(SeverityLevel.NORMAL)
    @Story("Search Flights")
    @Description("Select passengers and search for available flights")
    public void flightSearchTest() {
        log.info("🔍 Searching for flights...");
        FlightsSearchPage searchPage = new FlightsSearchPage(driver);
        Assert.assertTrue(searchPage.isAt(), "Flights Search page did not load.");

        searchPage.selectPassengers(testData.passengersCount());
        searchPage.searchForFlights();
    }

    @Test(dependsOnMethods = "flightSearchTest", description = "Flight class selection and confirmation")
    @Severity(SeverityLevel.NORMAL)
    @Story("Select Flights")
    @Description("Select departure and arrival flights and confirm selection")
    public void flightsSelectionTest() {
        log.info("✈️ Selecting flight class...");
        FlightClassSelectionPage classSelectionPage = new FlightClassSelectionPage(driver);
        Assert.assertTrue(classSelectionPage.isAt(), "Flight class selection page did not load.");

        classSelectionPage.selectFlights();
        classSelectionPage.confirmFlights();
    }

    @Test(dependsOnMethods = "flightsSelectionTest", description = "Flight booking confirmation and price verification")
    @Severity(SeverityLevel.CRITICAL)
    @Story("Confirm Booking")
    @Description("Validate final booking and check price")
    public void flightReservationConfirmationTest() {
        log.info("✅ Verifying flight reservation confirmation...");
        FlightConfirmationPage confirmationPage = new FlightConfirmationPage(driver);
        Assert.assertTrue(confirmationPage.isAt(), "Flight confirmation page did not load.");

        String actualPrice = confirmationPage.getPrice();
        Assert.assertEquals(actualPrice, testData.expectedPrice(), "Price mismatch on confirmation.");
    }
}
