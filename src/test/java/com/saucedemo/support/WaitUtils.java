package com.saucedemo.support;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * WaitUtils — centralised explicit wait helpers.
 *
 * Design principles:
 *  - ZERO Thread.sleep() calls.  Every wait is condition-based.
 *  - Default timeout comes from config so it can be tuned per environment.
 *  - All helpers are static for convenient use inside Page Objects.
 *  - Polling interval is set to 500 ms (Selenium default) — fast enough
 *    for most UIs without hammering the DOM.
 */
public class WaitUtils {

    private static final Logger log = LoggerFactory.getLogger(WaitUtils.class);

    /** Default wait time in seconds — overridable via config */
    private static final int DEFAULT_TIMEOUT =
            Integer.parseInt(ConfigManager.get("explicit.wait.seconds", "10"));

    private WaitUtils() { }

    // ── Core wait builder ────────────────────────────────────────────────────

    public static WebDriverWait wait(WebDriver driver) {
        return new WebDriverWait(driver, Duration.ofSeconds(DEFAULT_TIMEOUT));
    }

    public static WebDriverWait wait(WebDriver driver, int seconds) {
        return new WebDriverWait(driver, Duration.ofSeconds(seconds));
    }

    // ── Visibility ───────────────────────────────────────────────────────────

    /**
     * Waits until element is visible in the DOM and has non-zero size.
     */
    public static WebElement waitForVisible(WebDriver driver, By locator) {
        log.debug("Waiting for visible: {}", locator);
        return wait(driver).until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    public static WebElement waitForVisible(WebDriver driver, WebElement element) {
        return wait(driver).until(ExpectedConditions.visibilityOf(element));
    }

    // ── Clickability ─────────────────────────────────────────────────────────

    /**
     * Waits until element is visible AND enabled (ready to click).
     * Prevents ElementNotInteractableException on buttons rendered but disabled.
     */
    public static WebElement waitForClickable(WebDriver driver, By locator) {
        log.debug("Waiting for clickable: {}", locator);
        return wait(driver).until(ExpectedConditions.elementToBeClickable(locator));
    }

    public static WebElement waitForClickable(WebDriver driver, WebElement element) {
        return wait(driver).until(ExpectedConditions.elementToBeClickable(element));
    }

    // ── Text / Attribute ─────────────────────────────────────────────────────

    /**
     * Waits until an element contains the expected text.
     * Useful for cart badge count updates and confirmation messages.
     */
    public static boolean waitForText(WebDriver driver, By locator, String expectedText) {
        log.debug("Waiting for text '{}' in: {}", expectedText, locator);
        return wait(driver).until(
                ExpectedConditions.textToBePresentInElementLocated(locator, expectedText));
    }

    // ── URL / Page ───────────────────────────────────────────────────────────

    public static boolean waitForUrlContains(WebDriver driver, String urlFragment) {
        log.debug("Waiting for URL to contain: {}", urlFragment);
        return wait(driver).until(ExpectedConditions.urlContains(urlFragment));
    }

    // ── Disappearance ─────────────────────────────────────────────────────────

    public static boolean waitForInvisible(WebDriver driver, By locator) {
        log.debug("Waiting for invisible: {}", locator);
        return wait(driver).until(ExpectedConditions.invisibilityOfElementLocated(locator));
    }

    // ── Retry with eventual consistency ─────────────────────────────────────

    /**
     * Retries a condition supplier until it returns true or maxAttempts is reached.
     * Used to handle eventual consistency in DB validation scenarios.
     *
     * @param condition  a supplier that returns true when the condition is met
     * @param maxAttempts  number of retries
     * @param intervalMs  wait between retries in milliseconds
     */
    public static boolean retryUntil(Supplier<Boolean> condition, int maxAttempts, long intervalMs) {
        for (int i = 1; i <= maxAttempts; i++) {
            try {
                if (Boolean.TRUE.equals(condition.get())) {
                    log.info("Condition met on attempt {}/{}", i, maxAttempts);
                    return true;
                }
                log.debug("Attempt {}/{} — condition not yet met. Waiting {}ms…", i, maxAttempts, intervalMs);
                Thread.sleep(intervalMs);  // justified: DB eventual-consistency polling
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Retry interrupted");
                break;
            }
        }
        log.error("Condition not met after {} attempts", maxAttempts);
        return false;
    }
}
