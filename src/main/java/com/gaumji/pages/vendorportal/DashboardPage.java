package com.gaumji.pages.vendorportal;

import com.gaumji.pages.AbstractPage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.slf4j.ILoggerFactory;
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
    public boolean isAt() {
        this.wait.until(ExpectedConditions.visibilityOf(monthlyEarningElement));
        return this.monthlyEarningElement.isDisplayed();
    }

    public String getMonthlyEarning(){
       return this.monthlyEarningElement.getText();
    }

    public String getAnnualEarning(){
        return this.annualEarningElement.getText();
    }

    public String getProfitMargin(){
        return this.profitMarginElement.getText();
    }

    public String getAvailableInventory(){
        return this.availableInventoryElement.getText();
    }

    public String getSearchResultsCount(){
        return this.searchResultsCountElement.getText();
    }

    public void searchOrderHistory(String searchKeyword){
        this.searchInput.sendKeys(searchKeyword);
    }

    public int getResultsCount(){
        String resultsCountText = this.searchResultsCountElement.getText();
        String[] arr = resultsCountText.split(" ");
        int count = Integer.parseInt(arr[5]);
        log.info("Results count: {}", count);
        return count;
    }

    public void logout(){
        this.profilePictureElement.click();
        this.logoutLink.click();
        this.wait.until(ExpectedConditions.visibilityOf(modalLogoutButton));
        this.modalLogoutButton.click();
    }

}
