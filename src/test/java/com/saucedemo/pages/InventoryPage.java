package com.saucedemo.pages;

import com.saucedemo.support.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;

/**
 * InventoryPage — Page Object for the products listing page (/inventory.html).
 *
 * Locator Strategy for "Sauce Labs Backpack":
 *
 * PRIMARY (implemented below):
 *   Find the product by its name text within a data-test container:
 *     By.xpath("//div[@data-test='inventory-item-name' and text()='Sauce Labs Backpack']
 *              /ancestor::div[@data-test='inventory-item']
 *              //button[contains(@data-test,'add-to-cart')]")
 *
 *   Why: text-based resolution is resilient to DOM position changes.
 *   The data-test attribute on the parent container is stable.
 *   We walk up to the item container, then down to its specific button —
 *   avoiding any index-based selection.
 *
 * FALLBACK:
 *   By.cssSelector("[data-test='add-to-cart-sauce-labs-backpack']")
 *   This is the actual data-test value on the button. Simple, fast, and
 *   stable as long as the product slug doesn't change. Less flexible
 *   for parameterised tests where we don't know the slug ahead of time.
 *
 * AVOIDED:
 *   • (//button)[3]          — index-based, breaks if product order changes
 *   • .btn_inventory:nth-child(1) — CSS child index, same problem
 *   • //button[@class='...'] — class-based, breaks on style refactors
 */
public class InventoryPage {

    private static final Logger log = LoggerFactory.getLogger(InventoryPage.class);
    private final WebDriver driver;

    // ── Locators ─────────────────────────────────────────────────────────────

    private static final By PRODUCTS_TITLE  = By.cssSelector("[data-test='title']");
    private static final By CART_BADGE      = By.cssSelector("[data-test='shopping-cart-badge']");
    private static final By CART_ICON       = By.className("shopping_cart_link");
    private static final By PRODUCT_NAMES   = By.cssSelector("[data-test='inventory-item-name']");

    public InventoryPage(WebDriver driver) {
        this.driver = driver;
    }

    // ── Page actions ─────────────────────────────────────────────────────────

    public boolean isLoaded() {
        try {
            WaitUtils.waitForVisible(driver, PRODUCTS_TITLE);
            return driver.getCurrentUrl().contains("inventory");
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Adds a product to cart by its display name.
     *
     * Strategy: Locate the product name element by text → traverse up to
     * the inventory-item container → locate the Add to Cart button within
     * that container. This avoids index-based selection entirely.
     *
     * @param productName exact display name, e.g. "Sauce Labs Backpack"
     */
    public void addToCartByName(String productName) {
        log.info("Adding product to cart: '{}'", productName);

        // PRIMARY locator — text-based XPath with ancestor traversal
        By addButtonLocator = By.xpath(
            "//div[@data-test='inventory-item-name' and text()='" + productName + "']" +
            "/ancestor::div[@data-test='inventory-item']" +
            "//button[contains(@data-test,'add-to-cart')]"
        );

        WebElement addButton = WaitUtils.waitForClickable(driver, addButtonLocator);
        addButton.click();
        log.info("Added '{}' to cart", productName);
    }

    public String getCartBadgeCount() {
        // Badge only appears after item added — wait up to 5 seconds
        try {
            return WaitUtils.wait(driver, 5)
                    .until(ExpectedConditions.visibilityOfElementLocated(CART_BADGE))
                    .getText();
        } catch (Exception e) {
            return "0";
        }
    }

    public boolean isCartBadgeVisible() {
        try {
            return driver.findElement(CART_BADGE).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public void goToCart() {
        log.info("Navigating to cart");
        WebElement cartIcon = WaitUtils.waitForVisible(driver, CART_ICON);
        // Use JavaScript click to bypass any invisible overlay blocking the element
        ((org.openqa.selenium.JavascriptExecutor) driver).executeScript("arguments[0].click();", cartIcon);
        log.info("Cart icon clicked via JS");
    }

    public List<WebElement> getAllProductNames() {
        WaitUtils.waitForVisible(driver, PRODUCT_NAMES);
        return driver.findElements(PRODUCT_NAMES);
    }
}
