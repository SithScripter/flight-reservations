package com.gaumji.pages.flightreservation;

import com.gaumji.pages.AbstractPage;
import io.qameta.allure.Step;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class FlightClassSelectionPage extends AbstractPage {

    private static final Logger log = LoggerFactory.getLogger(FlightClassSelectionPage.class);

    @FindBy(name = "departure-flight")
    private List<WebElement> departureFlightClassOptions;

    @FindBy(name = "arrival-flight")
    private List<WebElement> arrivalFlightClassOptions;

    @FindBy(id = "confirm-flights")
    private WebElement confirmFlightsButton;

    public FlightClassSelectionPage(WebDriver driver) {
        super(driver);
    }

    @Override
    @Step("Verify if Flight Class Selection Page is displayed")
    public boolean isAt() {
        this.wait.until(ExpectedConditions.visibilityOf(this.confirmFlightsButton));
        boolean visible = this.confirmFlightsButton.isDisplayed();
        log.info("Flight Class Selection Page isAt check: {}", visible);
        return visible;
    }

    @Step("Select random departure and arrival flight classes")
    public void selectFlights() {
        int randomIndex = ThreadLocalRandom.current().nextInt(0, departureFlightClassOptions.size());
        // Log selection
        log.info("Selecting flight options at index: {}", randomIndex);

        // Click on departure flight class
        departureFlightClassOptions.get(randomIndex).click();
        log.info("Departure flight class selected");

        this.wait.until(ExpectedConditions.elementToBeClickable(arrivalFlightClassOptions.get(randomIndex)));

        // Click on arrival flight class using JS
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("arguments[0].click();", arrivalFlightClassOptions.get(randomIndex));
        log.info("Arrival flight class selected via JavaScript");
    }

    @Step("Confirm selected flights")
    public void confirmFlights() {
        log.info("Confirming selected flights...");
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("arguments[0].click();", confirmFlightsButton);
        log.info("Clicked confirm flights button");
    }
}
