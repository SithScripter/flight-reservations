package com.gaumji.pages.flightreservation;

import com.gaumji.pages.AbstractPage;
import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RegistrationConfirmationPage extends AbstractPage {

    private static final Logger log = LoggerFactory.getLogger(RegistrationConfirmationPage.class);

    @FindBy(id = "go-to-flights-search")
    private WebElement flightSearchButton;

    @FindBy(css = "p.mt-3 > b")
    private WebElement firstNameElement;

    public RegistrationConfirmationPage(WebDriver driver) {
        super(driver);
    }

    @Override
    @Step("Verifying Registration Confirmation page is loaded")
    public boolean isAt() {
        this.wait.until(ExpectedConditions.visibilityOf(this.flightSearchButton));
        boolean isVisible = this.flightSearchButton.isDisplayed();
        log.info("Registration Confirmation page visibility: {}", isVisible);
        return isVisible;
    }

    @Step("Navigating to Flights Search Page")
    public void goToFlightSearch() {
        log.info("Clicking 'Go to Flights Search' button");
        this.flightSearchButton.click();
    }

    @Step("Retrieving registered user's first name")
    public String getFirstName() {
        String name = this.firstNameElement.getText();
        log.info("Registered First Name: {}", name);
        return name;
    }
}
