package com.gaumji.pages.vendorportal;

import com.gaumji.pages.AbstractPage;
import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DashboardPage extends AbstractPage {

    private static final Logger log = LoggerFactory.getLogger(DashboardPage.class);

    @FindBy(id = "monthly-earning")
    private WebElement monthlyEarningElement;

    @FindBy(id = "annual-earning")
    private WebElement annualEarningElement;

    @FindBy(id = "profit-margin")
    private WebElement profitMarginElement;

    @FindBy(id = "available-inventory")
    private WebElement availableInventoryElement;

    @FindBy(css = "input[type*='search']")
    private WebElement searchInput;

    @FindBy(id = "dataTable_info")
    private WebElement searchResultsCountElement;

    @FindBy(css = ".img-profile")
    private WebElement profilePictureElement;

    @FindBy(css = "a[data-toggle='modal']")
    private WebElement logoutLink;

    @FindBy(css = "a[class='btn btn-primary']")
    private WebElement modalLogoutButton;

    public DashboardPage(WebDriver driver) {
        super(driver);
    }

    @Override
    @Step("Verifying Dashboard Page is loaded")
    public boolean isAt() {
        this.wait.until(ExpectedConditions.visibilityOf(monthlyEarningElement));
        boolean isDisplayed = this.monthlyEarningElement.isDisplayed();
        log.info("Dashboard loaded: {}", isDisplayed);
        return isDisplayed;
    }

    @Step("Fetching Monthly Earning")
    public String getMonthlyEarning() {
        String value = this.monthlyEarningElement.getText();
        log.info("Monthly Earning: {}", value);
        return value;
    }

    @Step("Fetching Annual Earning")
    public String getAnnualEarning() {
        String value = this.annualEarningElement.getText();
        log.info("Annual Earning: {}", value);
        return value;
    }

    @Step("Fetching Profit Margin")
    public String getProfitMargin() {
        String value = this.profitMarginElement.getText();
        log.info("Profit Margin: {}", value);
        return value;
    }

    @Step("Fetching Available Inventory")
    public String getAvailableInventory() {
        String value = this.availableInventoryElement.getText();
        log.info("Available Inventory: {}", value);
        return value;
    }

    @Step("Getting search results count text")
    public String getSearchResultsCount() {
        String value = this.searchResultsCountElement.getText();
        log.info("Search Result Count (raw): {}", value);
        return value;
    }

    @Step("Searching Order History with keyword: {searchKeyword}")
    public void searchOrderHistory(String searchKeyword) {
        log.info("Searching for keyword: {}", searchKeyword);
        this.searchInput.sendKeys(searchKeyword);
    }

    @Step("Extracting actual result count from text")
    public int getResultsCount() {
        String resultsCountText = this.searchResultsCountElement.getText();
        String[] arr = resultsCountText.split(" ");
        int count = Integer.parseInt(arr[5]);
        log.info("Parsed Results Count: {}", count);
        return count;
    }

    @Step("Logging out from Dashboard")
    public void logout() {
        log.info("Logging out...");
        this.profilePictureElement.click();
        this.logoutLink.click();
        this.wait.until(ExpectedConditions.visibilityOf(modalLogoutButton));
        this.modalLogoutButton.click();
    }
}
