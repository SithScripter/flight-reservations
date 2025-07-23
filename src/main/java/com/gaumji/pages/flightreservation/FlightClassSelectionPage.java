package com.gaumji.pages.flightreservation;

import com.gaumji.pages.AbstractPage;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class FlightClassSelectionPage extends AbstractPage {

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
    public boolean isAt() {
        this.wait.until(ExpectedConditions.visibilityOf(this.confirmFlightsButton));
        return this.confirmFlightsButton.isDisplayed();
    }

    public void selectFlights(){
        int random = ThreadLocalRandom.current().nextInt(0, departureFlightClassOptions.size());
        this.departureFlightClassOptions.get(random).click();
//        this.arrivalFlightClassOptions.get(random).click();

        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("arguments[0].click();", arrivalFlightClassOptions.get(random));

    }

    public void confirmFlights(){
//        this.confirmFlightsButton.click();
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("arguments[0].click();", confirmFlightsButton);
    }
}
