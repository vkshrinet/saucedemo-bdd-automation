package com.saucedemo.pages;

import com.saucedemo.support.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CheckoutStepTwoPage {

    private static final Logger log = LoggerFactory.getLogger(CheckoutStepTwoPage.class);
    private final WebDriver driver;

    private static final By SUMMARY_TITLE = By.cssSelector("[data-test='title']");
    private static final By FINISH_BUTTON = By.cssSelector("[data-test='finish']");

    public CheckoutStepTwoPage(WebDriver driver) {
        this.driver = driver;
    }

    public boolean isLoaded() {
        try {
            WaitUtils.waitForVisible(driver, SUMMARY_TITLE);
            return driver.getCurrentUrl().contains("checkout-step-two");
        } catch (Exception e) {
            return false;
        }
    }

    public void finishOrder() {
        log.info("Clicking Finish to complete order");
        WaitUtils.waitForUrlContains(driver, "checkout-step-two");
        WebElement finishBtn = WaitUtils.waitForClickable(driver, FINISH_BUTTON);
        ((org.openqa.selenium.JavascriptExecutor) driver)
            .executeScript("arguments[0].click();", finishBtn);
        WaitUtils.waitForUrlContains(driver, "checkout-complete");
        log.info("Order completed - on confirmation page");
    }
}
