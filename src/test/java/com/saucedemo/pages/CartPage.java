package com.saucedemo.pages;

import com.saucedemo.support.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CartPage {

    private static final Logger log = LoggerFactory.getLogger(CartPage.class);
    private final WebDriver driver;

    private static final By CART_TITLE      = By.cssSelector("[data-test='title']");
    private static final By CART_ITEM       = By.cssSelector("[data-test='inventory-item']");
    private static final By CHECKOUT_BUTTON = By.cssSelector("[data-test='checkout']");

    public CartPage(WebDriver driver) {
        this.driver = driver;
    }

    public boolean isLoaded() {
        try {
            WaitUtils.waitForUrlContains(driver, "cart");
            WaitUtils.waitForVisible(driver, CART_TITLE);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public int getCartItemCount() {
        return driver.findElements(CART_ITEM).size();
    }

    public void proceedToCheckout() {
        log.info("Proceeding to checkout");
        WebElement checkoutBtn = WaitUtils.waitForClickable(driver, CHECKOUT_BUTTON);
        ((org.openqa.selenium.JavascriptExecutor) driver)
            .executeScript("arguments[0].click();", checkoutBtn);
        WaitUtils.waitForUrlContains(driver, "checkout-step-one");
        log.info("Navigated to checkout step one");
    }
}
