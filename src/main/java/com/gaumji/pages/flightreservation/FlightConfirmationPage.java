package com.gaumji.pages.flightreservation;

import com.gaumji.pages.AbstractPage;
import io.qameta.allure.Step;
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
    @Step("Check if Flight Confirmation Page is displayed")
    public boolean isAt() {
        this.wait.until(ExpectedConditions.visibilityOf(this.flightConfirmationElement));
        boolean isDisplayed = this.flightConfirmationElement.isDisplayed();
        log.info("Flight Confirmation Page displayed: {}", isDisplayed);
        return isDisplayed;
    }

    @Step("Get flight confirmation number")
    public String getFlightConfirmationNumber() {
        String confirmation = this.flightConfirmationElement.getText();
        log.info("Flight Confirmation #: {}", confirmation);
        return confirmation;
    }

    @Step("Get total flight price")
    public String getPrice() {
        String price = this.totalPriceElement.getText();
        log.info("Total Price: {}", price);
        return price;
    }
}
