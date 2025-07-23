package com.gaumji.tests.flightreservations;

import com.gaumji.pages.flightreservation.*;
import com.gaumji.tests.AbstractTest;
import com.gaumji.tests.flightreservations.model.FlightReservationTestData;
import com.gaumji.util.Config;
import com.gaumji.util.Constants;
import com.gaumji.util.JsonUtil;
import org.testng.Assert;
import org.testng.annotations.*;

public class FlightReservationTest extends AbstractTest {

    private FlightReservationTestData testData;

    @BeforeTest
    @Parameters("testDataPath")
    public void setParameters(String testDataPath) {
        this.testData = JsonUtil.getData(testDataPath, FlightReservationTestData.class);
    }

    @Test
    public void userRegistrationTest() {
        RegistrationPage registrationPage = new RegistrationPage(driver);
        registrationPage.goTo(Config.get(Constants.FLIGHT_RESERVATION_URL));
        Assert.assertTrue(registrationPage.isAt());

        registrationPage.enterUserDetails(testData.firstname(), testData.lastname());
        registrationPage.enterUserCredentials(testData.email(), testData.password());
        registrationPage.enterUserAddress(testData.street(), testData.city(), testData.zip());
        registrationPage.register();
    }

    @Test(dependsOnMethods = "userRegistrationTest")
    public void registrationConfirmationTest() {
        RegistrationConfirmationPage registrationConfirmationPage = new RegistrationConfirmationPage(driver);
        Assert.assertTrue(registrationConfirmationPage.isAt());
        Assert.assertEquals(registrationConfirmationPage.getFirstName(), testData.firstname());
        registrationConfirmationPage.goToFlightSearch();
    }

    @Test(dependsOnMethods = "registrationConfirmationTest")
    public void flightSearchTest() {
        FlightsSearchPage flightsSearchPage = new FlightsSearchPage(driver);
        Assert.assertTrue(flightsSearchPage.isAt());

        flightsSearchPage.selectPassengers(testData.passengersCount());
        flightsSearchPage.searchForFlights();
    }

    @Test(dependsOnMethods = "flightSearchTest")
    public void flightsSelectionTest() {
        FlightClassSelectionPage flightClassSelectionPage = new FlightClassSelectionPage(driver);
        Assert.assertTrue(flightClassSelectionPage.isAt());
        flightClassSelectionPage.selectFlights();
        flightClassSelectionPage.confirmFlights();
    }

    @Test(dependsOnMethods = "flightsSelectionTest")
    public void flightReservationConfirmationTest() {
        FlightConfirmationPage flightConfirmationPage = new FlightConfirmationPage(driver);
        Assert.assertTrue(flightConfirmationPage.isAt());
        Assert.assertEquals(flightConfirmationPage.getPrice(), testData.expectedPrice());
    }
}
