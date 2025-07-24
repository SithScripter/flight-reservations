package com.gaumji.pages.vendorportal;

import com.gaumji.pages.AbstractPage;
import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoginPage extends AbstractPage {

    private static final Logger log = LoggerFactory.getLogger(LoginPage.class);

    @FindBy(id = "username")
    private WebElement userNameInput;

    @FindBy(id = "password")
    private WebElement passwordInput;

    @FindBy(id = "login")
    private WebElement loginButton;

    public LoginPage(WebDriver driver) {
        super(driver);
    }

    @Override
    @Step("Verifying Login Page is loaded")
    public boolean isAt() {
        this.wait.until(ExpectedConditions.visibilityOf(userNameInput));
        boolean isDisplayed = this.loginButton.isDisplayed();
        log.info("Login page loaded: {}", isDisplayed);
        return isDisplayed;
    }

    @Step("Navigating to URL: {url}")
    public void goTo(String url) {
        log.info("Navigating to {}", url);
        this.driver.get(url);
    }

    @Step("Logging in with Username: {username}")
    public void login(String username, String password) {
        log.info("Filling in credentials and clicking login");
        this.userNameInput.sendKeys(username);
        this.passwordInput.sendKeys(password);
        this.loginButton.click();
    }
}
