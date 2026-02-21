package com.saucedemo.steps;

import com.fasterxml.jackson.databind.JsonNode;
import com.saucedemo.pages.*;
import com.saucedemo.support.ConfigManager;
import com.saucedemo.support.DriverFactory;
import com.saucedemo.support.TestDataLoader;
import com.saucedemo.support.WaitUtils;

import io.cucumber.java.en.*;
import org.junit.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PurchaseSteps — step definitions for the E2E purchase flow.
 *
 * Rules enforced here:
 *  - NO locators in this file (all in Page Objects)
 *  - NO direct driver interactions (all through Page Objects)
 *  - Test data comes from TestDataLoader (testdata/users.json, testdata/checkout.json)
 *  - Assertions use JUnit's Assert for clear failure messages
 */
public class PurchaseSteps {

    private static final Logger log = LoggerFactory.getLogger(PurchaseSteps.class);

    private WebDriver driver;
    private LoginPage loginPage;
    private InventoryPage inventoryPage;
    private CartPage cartPage;
    private CheckoutStepOnePage checkoutStepOne;
    private CheckoutStepTwoPage checkoutStepTwo;
    private OrderConfirmationPage confirmationPage;

    private final String baseUrl = ConfigManager.get("base.url", "https://www.saucedemo.com");

    private void initPages() {
        driver = DriverFactory.getDriver();
        loginPage         = new LoginPage(driver);
        inventoryPage     = new InventoryPage(driver);
        cartPage          = new CartPage(driver);
        checkoutStepOne   = new CheckoutStepOnePage(driver);
        checkoutStepTwo   = new CheckoutStepTwoPage(driver);
        confirmationPage  = new OrderConfirmationPage(driver);
    }

    // ── Background ────────────────────────────────────────────────────────────

    @Given("I am on the SauceDemo login page")
    public void iAmOnTheSauceDemoLoginPage() {
        initPages();
        loginPage.navigateTo(baseUrl);
        log.info("On login page: {}", baseUrl);
    }

    // ── Login steps ───────────────────────────────────────────────────────────

    @Given("I login as a {string}")
    public void iLoginAs(String userKey) {
        // Load credentials from testdata/users.json — no hardcoding
        JsonNode user = TestDataLoader.get("users.json", userKey);
        String username = user.get("username").asText();
        String password = user.get("password").asText();

        log.info("Logging in as: {}", userKey);
        loginPage.login(username, password);

        Assert.assertTrue("Products page should be visible after login",
                inventoryPage.isLoaded());
    }

    // ── Inventory steps ───────────────────────────────────────────────────────

    @When("I add {string} to the cart")
    public void iAddProductToCart(String productName) {
        inventoryPage.addToCartByName(productName);
    }

    @Then("the cart badge should display {string}")
    public void theCartBadgeShouldDisplay(String expectedCount) {
        WaitUtils.waitForText(driver, 
            By.cssSelector("[data-test='shopping-cart-badge']"), expectedCount);
        String actual = inventoryPage.getCartBadgeCount();
        Assert.assertEquals("Cart badge count mismatch", expectedCount, actual);
    }

    @When("I proceed to checkout")
    public void iProceedToCheckout() {
        inventoryPage.goToCart();
        WaitUtils.waitForUrlContains(driver, "cart");
        Assert.assertTrue("Cart page should be visible", cartPage.isLoaded());
        cartPage.proceedToCheckout();
    }

    // ── Checkout steps ────────────────────────────────────────────────────────

    @And("I enter checkout details for {string}")
    public void iEnterCheckoutDetailsFor(String customerKey) {
        JsonNode customer = TestDataLoader.get("checkout.json", customerKey);
        checkoutStepOne.enterCustomerDetails(
                customer.get("firstName").asText(),
                customer.get("lastName").asText(),
                customer.get("postalCode").asText()
        );
    }

    @And("I complete the order")
    public void iCompleteTheOrder() {
        checkoutStepTwo.finishOrder();
    }

    // ── Confirmation steps ────────────────────────────────────────────────────

    @Then("I should see the order confirmation page")
    public void iShouldSeeTheOrderConfirmationPage() {
        Assert.assertTrue("Order confirmation page should be visible",
                confirmationPage.isLoaded());
    }

    @And("the confirmation message should be {string}")
    public void theConfirmationMessageShouldBe(String expectedMessage) {
        String actual = confirmationPage.getConfirmationHeader();
        Assert.assertEquals("Confirmation message mismatch", expectedMessage, actual);
        log.info("Order confirmed: '{}'", actual);
    }
}
