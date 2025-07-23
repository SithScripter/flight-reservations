package com.gaumji.pages.flightreservation;

import com.gaumji.pages.AbstractPage;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;

public class FlightsSearchPage extends AbstractPage {

    @FindBy(id = "passengers")
    private WebElement passengerSelect;

    @FindBy(id = "search-flights")
    private WebElement searchFlightsButton;

    public FlightsSearchPage(WebDriver driver) {
        super(driver);
    }

    @Override
    public boolean isAt() {
        this.wait.until(ExpectedConditions.visibilityOf(this.passengerSelect));
        return this.passengerSelect.isDisplayed();
    }

    public void selectPassengers(String noOfPassenger){
        Select passengers = new Select(this.passengerSelect);
        passengers.selectByValue(noOfPassenger);
    }

    public void searchForFlights(){
//        this.searchFlightsButton.click();
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("arguments[0].click();", searchFlightsButton);
    }




}
