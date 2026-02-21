package com.saucedemo.support;

import io.cucumber.java.After;
import io.cucumber.java.AfterStep;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cucumber Hooks — executed automatically around every scenario.
 *
 * Responsibilities:
 *  - @Before : initialise driver and navigate to base URL
 *  - @After  : capture screenshot on failure, quit driver
 *
 * The order value ensures Hooks run before any step-definition @Before.
 */
public class Hooks {

    private static final Logger log = LoggerFactory.getLogger(Hooks.class);

    @Before(order = 1)
    public void setUp(Scenario scenario) {
        log.info("Starting scenario: [{}]", scenario.getName());
        DriverFactory.initDriver();
    }

    /**
     * Captures a screenshot after EVERY failing step — not just at scenario end —
     * giving a precise view of exactly which step caused the failure.
     */
    @AfterStep
    public void afterStep(Scenario scenario) {
        if (scenario.isFailed()) {
            captureScreenshot(scenario, "step-failure");
        }
    }

    @After(order = 1)
    public void tearDown(Scenario scenario) {
        if (scenario.isFailed()) {
            captureScreenshot(scenario, "scenario-failure");
            log.error("Scenario FAILED: [{}]", scenario.getName());
        } else {
            log.info("Scenario PASSED: [{}]", scenario.getName());
        }
        DriverFactory.quitDriver();
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private void captureScreenshot(Scenario scenario, String label) {
        try {
            WebDriver driver = DriverFactory.getDriver();
            byte[] screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
            scenario.attach(screenshot, "image/png",
                    label + "_" + scenario.getName().replaceAll("\\s+", "_"));
            log.info("Screenshot captured: {}", label);
        } catch (Exception e) {
            log.warn("Could not capture screenshot: {}", e.getMessage());
        }
    }
}
