package com.gaumji.pages.flightreservation;

import com.gaumji.pages.AbstractPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class RegistrationConfirmationPage extends AbstractPage {

    @FindBy(id = "go-to-flights-search")
    private WebElement flightSearchButton;

    @FindBy(css = "p.mt-3 > b")
    private WebElement firstNameElement;

    public RegistrationConfirmationPage(WebDriver driver){
        super(driver);
    }

    @Override
    public boolean isAt() {
        this.wait.until(ExpectedConditions.visibilityOf(this.flightSearchButton));
        return this.flightSearchButton.isDisplayed();
    }

    public void goToFlightSearch(){
        this.flightSearchButton.click();
    }

    public String getFirstName(){
        return this.firstNameElement.getText();
    }
}
