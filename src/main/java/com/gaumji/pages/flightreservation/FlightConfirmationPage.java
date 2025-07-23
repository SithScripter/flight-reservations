package com.gaumji.pages.flightreservation;

import com.gaumji.pages.AbstractPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlightConfirmationPage extends AbstractPage {

    private static final Logger log = LoggerFactory.getLogger(FlightConfirmationPage.class);

    @FindBy(css = "div[class$='row']:nth-child(1) div[class='col']:last-child")
    private WebElement flightConfirmationElement;

    @FindBy(css = "div[class$='row']:nth-child(3) div[class='col']:last-child")
    private WebElement totalPriceElement;

    public FlightConfirmationPage(WebDriver driver) {

        super(driver);
    }

    @Override
    public boolean isAt() {
        this.wait.until(ExpectedConditions.visibilityOf(this.flightConfirmationElement));
        return this.flightConfirmationElement.isDisplayed();
    }

    public String getPrice(){
        String confirmation = this.flightConfirmationElement.getText();
        String price = this.totalPriceElement.getText();
        log.info("Flight Confirmation# : {}", confirmation);
        log.info("Total Price : {}", price);
        return price;
    }
}
