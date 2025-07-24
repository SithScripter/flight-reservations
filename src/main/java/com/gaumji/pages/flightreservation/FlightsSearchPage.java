package com.gaumji.pages.flightreservation;

import com.gaumji.pages.AbstractPage;
import io.qameta.allure.Step;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlightsSearchPage extends AbstractPage {

    private static final Logger log = LoggerFactory.getLogger(FlightsSearchPage.class);

    @FindBy(id = "passengers")
    private WebElement passengerSelect;

    @FindBy(id = "search-flights")
    private WebElement searchFlightsButton;

    public FlightsSearchPage(WebDriver driver) {
        super(driver);
    }

    @Override
    @Step("Verify if Flights Search Page is displayed")
    public boolean isAt() {
        this.wait.until(ExpectedConditions.visibilityOf(this.passengerSelect));
        boolean visible = this.passengerSelect.isDisplayed();
        log.info("Flights Search Page isAt check: {}", visible);
        return visible;
    }

    @Step("Selecting number of passengers: {noOfPassenger}")
    public void selectPassengers(String noOfPassenger) {
        Select passengers = new Select(this.passengerSelect);
        passengers.selectByValue(noOfPassenger);
        log.info("Selected {} passengers", noOfPassenger);
    }

    @Step("Searching for available flights")
    public void searchForFlights() {
        log.info("Clicking Search Flights button");
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("arguments[0].click();", searchFlightsButton);
        log.info("Search Flights button clicked");
    }
}
