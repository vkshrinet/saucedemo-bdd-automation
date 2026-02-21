package com.saucedemo.pages;

import com.saucedemo.support.DriverFactory;
import com.saucedemo.support.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LoginPage — Page Object for https://www.saucedemo.com/
 *
 * Locator strategy (see README for full explanation):
 *
 * PRIMARY — data-test attributes:
 *   The SauceDemo app ships with stable data-test="username" / data-test="password"
 *   attributes on its form fields. These are the ideal locator because:
 *     • They are decoupled from CSS styling (class renames don't break tests)
 *     • They are decoupled from DOM structure (position changes don't break tests)
 *     • They clearly communicate testing intent to developers
 *   Selector: [data-test="username"]   →   CSS attribute selector
 *
 * FALLBACK — id attributes:
 *   The inputs also have id="user-name" and id="password".
 *   IDs are unique by spec and highly stable, but less explicit than data-test.
 *   Selector: By.id("user-name")
 *
 * AVOIDED:
 *   • XPath by index  //input[1]  — breaks with DOM reordering
 *   • Class-based    .input_error  — changes with CSS refactors
 *   • Link text      — N/A on this page
 */
public class LoginPage {

    private static final Logger log = LoggerFactory.getLogger(LoginPage.class);
    private final WebDriver driver;

    // ── Locators (PRIMARY: data-test attributes) ─────────────────────────────
    private static final By USERNAME_INPUT  = By.cssSelector("[data-test='username']");
    private static final By PASSWORD_INPUT  = By.cssSelector("[data-test='password']");
    private static final By LOGIN_BUTTON    = By.cssSelector("[data-test='login-button']");
    private static final By ERROR_MESSAGE   = By.cssSelector("[data-test='error']");

    // Fallback locators (documented for transparency)
    // private static final By USERNAME_INPUT = By.id("user-name");
    // private static final By PASSWORD_INPUT = By.id("password");
    // private static final By LOGIN_BUTTON   = By.id("login-button");

    public LoginPage(WebDriver driver) {
        this.driver = driver;
    }

    // ── Page actions ─────────────────────────────────────────────────────────

    public void navigateTo(String baseUrl) {
        log.info("Navigating to login page: {}", baseUrl);
        driver.get(baseUrl);
        WaitUtils.waitForVisible(driver, USERNAME_INPUT);
    }

    public void enterUsername(String username) {
        log.debug("Entering username: {}", username);
        WaitUtils.waitForVisible(driver, USERNAME_INPUT).clear();
        driver.findElement(USERNAME_INPUT).sendKeys(username);
    }

    public void enterPassword(String password) {
        log.debug("Entering password");
        WaitUtils.waitForVisible(driver, PASSWORD_INPUT).clear();
        driver.findElement(PASSWORD_INPUT).sendKeys(password);
    }

    public void clickLogin() {
        log.info("Clicking login button");
        WaitUtils.waitForClickable(driver, LOGIN_BUTTON).click();
    }

    /**
     * Full login action — combines all three steps for happy-path scenarios.
     */
    public void login(String username, String password) {
        enterUsername(username);
        enterPassword(password);
        clickLogin();
    }

    // ── State inspection ─────────────────────────────────────────────────────

    public boolean isErrorDisplayed() {
        try {
            return WaitUtils.waitForVisible(driver, ERROR_MESSAGE).isDisplayed();
        } catch (Exception e) {
            return false;
        }
    }

    public String getErrorMessage() {
        return WaitUtils.waitForVisible(driver, ERROR_MESSAGE).getText();
    }

    public boolean isOnLoginPage() {
        return driver.getCurrentUrl().contains("saucedemo.com")
                && !driver.getCurrentUrl().contains("inventory");
    }
}
