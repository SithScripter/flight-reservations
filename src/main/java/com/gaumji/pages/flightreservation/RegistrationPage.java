package com.gaumji.pages.flightreservation;

import com.gaumji.pages.AbstractPage;
import io.qameta.allure.Step;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class RegistrationPage extends AbstractPage {

    @FindBy(id = "firstName")
    private WebElement firstNameInput;

    @FindBy(id = "lastName")
    private WebElement lastNameInput;

    @FindBy(id = "email")
    private WebElement emailInput;

    @FindBy(id = "password")
    private WebElement passwordInput;

    @FindBy(name = "street")
    private WebElement streetInput;

    @FindBy(name = "city")
    private WebElement cityInput;

    @FindBy(name = "zip")
    private WebElement zipInput;

    @FindBy(css = "#register-btn")
    private WebElement registerButton;

    public RegistrationPage(WebDriver driver){
        super(driver);
    }

    @Override
    @Step("Verify registration page is displayed")
    public boolean isAt() {
        this.wait.until(ExpectedConditions.visibilityOf(this.firstNameInput));
        return this.firstNameInput.isDisplayed();
    }

    @Step("Navigating to registration page: {url}")
    public void goTo(String url){
        this.driver.get(url);
    }

    @Step("Entering user details - First Name: {firstName}, Last Name: {lastName}")
    public void enterUserDetails(String firstName, String lastName){
        this.firstNameInput.sendKeys(firstName);
        this.lastNameInput.sendKeys(lastName);
    }

    @Step("Entering user credentials - Email: {userEmail}, Password: [PROTECTED]")
    public void enterUserCredentials(String userEmail, String userPassword){
        this.emailInput.sendKeys(userEmail);
        this.passwordInput.sendKeys(userPassword);
    }

    @Step("Entering address - Street: {street}, City: {city}, Zip: {zip}")
    public void enterUserAddress(String street, String city, String zip){
        this.streetInput.sendKeys(street);
        this.cityInput.sendKeys(city);
        this.zipInput.sendKeys(zip);
    }

    @Step("Clicking on Register button")
    public void register(){
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("arguments[0].click();", registerButton);
    }
}
