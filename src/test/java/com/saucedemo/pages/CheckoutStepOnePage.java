package com.saucedemo.pages;

import com.saucedemo.support.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CheckoutStepOnePage {

    private static final Logger log = LoggerFactory.getLogger(CheckoutStepOnePage.class);
    private final WebDriver driver;

    private static final By FIRST_NAME   = By.cssSelector("[data-test='firstName']");
    private static final By LAST_NAME    = By.cssSelector("[data-test='lastName']");
    private static final By POSTAL_CODE  = By.cssSelector("[data-test='postalCode']");
    private static final By CONTINUE_BTN = By.cssSelector("[data-test='continue']");

    public CheckoutStepOnePage(WebDriver driver) {
        this.driver = driver;
    }

    public void enterCustomerDetails(String firstName, String lastName, String postalCode) {
        log.info("Entering checkout details: {} {} {}", firstName, lastName, postalCode);
        WaitUtils.waitForVisible(driver, FIRST_NAME);

        fillReactInput(FIRST_NAME, firstName);
        fillReactInput(LAST_NAME, lastName);
        fillReactInput(POSTAL_CODE, postalCode);

        log.info("All fields filled. Clicking Continue...");
        WebElement continueBtn = WaitUtils.waitForClickable(driver, CONTINUE_BTN);
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("arguments[0].scrollIntoView({block:'center'});", continueBtn);
        js.executeScript("arguments[0].click();", continueBtn);

        WaitUtils.waitForUrlContains(driver, "checkout-step-two");
        log.info("Navigated to checkout step two");
    }

    /**
     * Fills a React-controlled input field reliably.
     * Standard sendKeys alone doesn't trigger React's onChange event,
     * so we use JS to set the value and then dispatch a real input event
     * to notify React's synthetic event system.
     */
    private void fillReactInput(By locator, String value) {
        WebElement field = driver.findElement(locator);
        JavascriptExecutor js = (JavascriptExecutor) driver;

        // Set value via React's internal property setter
        js.executeScript(
            "var nativeInputValueSetter = Object.getOwnPropertyDescriptor(" +
            "  window.HTMLInputElement.prototype, 'value').set;" +
            "nativeInputValueSetter.call(arguments[0], arguments[1]);" +
            "arguments[0].dispatchEvent(new Event('input', { bubbles: true }));",
            field, value
        );

        log.debug("Filled field {} with value via React setter", locator);
    }
}