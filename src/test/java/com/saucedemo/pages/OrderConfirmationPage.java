package com.saucedemo.pages;

import com.saucedemo.support.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OrderConfirmationPage {

    private static final Logger log = LoggerFactory.getLogger(OrderConfirmationPage.class);
    private final WebDriver driver;

    private static final By COMPLETE_HEADER  = By.cssSelector("[data-test='complete-header']");
    private static final By COMPLETE_TEXT    = By.cssSelector("[data-test='complete-text']");
    private static final By PONY_EXPRESS_IMG = By.cssSelector("[data-test='pony-express']");

    public OrderConfirmationPage(WebDriver driver) {
        this.driver = driver;
    }

    public boolean isLoaded() {
        try {
            WaitUtils.waitForVisible(driver, COMPLETE_HEADER);
            return driver.getCurrentUrl().contains("checkout-complete");
        } catch (Exception e) {
            return false;
        }
    }

    public String getConfirmationHeader() {
        return WaitUtils.waitForVisible(driver, COMPLETE_HEADER).getText();
    }

    public String getConfirmationText() {
        return driver.findElement(COMPLETE_TEXT).getText();
    }

    public boolean isConfirmationImageVisible() {
        try {
            return driver.findElement(PONY_EXPRESS_IMG).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }
}
