package com.saucedemo.steps;

import com.saucedemo.pages.LoginPage;
import com.saucedemo.support.ConfigManager;
import com.saucedemo.support.DriverFactory;
import io.cucumber.java.en.*;
import org.junit.Assert;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LoginSteps — step definitions for login validation scenarios.
 *
 * Shared step "I am on the SauceDemo login page" lives in PurchaseSteps
 * and is reused here via Cucumber's shared step registry.
 *
 * All assertions validate both the error message AND page state —
 * ensuring user truly remains on the login page, not silently redirected.
 */
public class LoginSteps {

    private static final Logger log = LoggerFactory.getLogger(LoginSteps.class);

    private WebDriver driver;
    private LoginPage loginPage;

    private final String baseUrl = ConfigManager.get("base.url", "https://www.saucedemo.com");

    private void initPages() {
        driver   = DriverFactory.getDriver();
        loginPage = new LoginPage(driver);
    }

    // ── Negative login ────────────────────────────────────────────────────────

    @Given("I attempt login with username {string} and password {string}")
    public void iAttemptLoginWithUsernameAndPassword(String username, String password) {
        initPages();
        loginPage.navigateTo(baseUrl);
        log.info("Attempting login with username: '{}' and [password]", username);
        loginPage.login(username, password);
    }

    @Then("I should see the error message {string}")
    public void iShouldSeeTheErrorMessage(String expectedError) {
        Assert.assertTrue("Error message should be visible", loginPage.isErrorDisplayed());
        String actualError = loginPage.getErrorMessage();
        Assert.assertEquals("Error message text mismatch", expectedError, actualError);
        log.info("Error message validated: '{}'", actualError);
    }

    @And("I should remain on the login page")
    public void iShouldRemainOnTheLoginPage() {
        Assert.assertTrue(
            "User should still be on login page after invalid credentials",
            loginPage.isOnLoginPage()
        );
        log.info("Confirmed: user remains on login page");
    }

    // ── Reused background step (delegates to PurchaseSteps) ──────────────────
    // Cucumber automatically shares steps across classes in the same package.
    // "Given I am on the SauceDemo login page" is already defined in PurchaseSteps.
}
