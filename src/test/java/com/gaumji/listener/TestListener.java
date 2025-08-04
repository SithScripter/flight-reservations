package com.gaumji.listener;

import com.gaumji.util.AllureEnvironmentWriter;
import com.gaumji.util.Constants;
import io.qameta.allure.Allure;
import io.qameta.allure.Attachment;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.testng.*;

public class TestListener implements ITestListener, IExecutionListener {

    @Override
    public void onTestFailure(ITestResult result) {
        WebDriver driver = (WebDriver) result.getTestContext().getAttribute(Constants.DRIVER);
        if (driver != null) {
            // Screenshot in TestNG HTML report
            String base64Screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BASE64);
            String htmlImage = String.format("<img width=700px src='data:image/png;base64,%s' />", base64Screenshot);
            Reporter.log(htmlImage);

            // Attach to Allure report
            saveScreenshotToAllure(driver);
        }

        // Add failure message to Allure
        saveFailureLogToAllure(result.getThrowable() != null ? result.getThrowable().toString() : "Unknown error");
    }

    @Override
    public void onExecutionFinish() {
        // ✅ Write final aggregated environment details to Allure
        AllureEnvironmentWriter.writeEnvironmentInfo();
    }

    @Attachment(value = "Screenshot", type = "image/png")
    private byte[] saveScreenshotToAllure(WebDriver driver) {
        return ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
    }

    @Attachment(value = "Failure Log", type = "text/plain")
    private String saveFailureLogToAllure(String message) {
        return message;
    }
}
