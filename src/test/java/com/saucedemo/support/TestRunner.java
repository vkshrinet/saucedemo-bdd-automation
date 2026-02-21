package com.saucedemo.support;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

/**
 * TestRunner — entry point for Maven Surefire and IDE execution.
 *
 * Run all tests:
 *   mvn test
 *
 * Run only @smoke tagged tests:
 *   mvn test -Dcucumber.filter.tags="@smoke"
 *
 * Run with Firefox in headless mode:
 *   mvn test -Dbrowser=firefox -Dheadless=true
 *
 * plugin list:
 *  - pretty       : human-readable console output
 *  - html         : Cucumber native HTML report → target/cucumber-reports/
 *  - json         : machine-readable → consumed by ExtentReports adapter
 *  - com.aventstack : ExtentReports integration (reads cucumber.properties)
 */
@RunWith(Cucumber.class)
@CucumberOptions(
    features   = "src/test/resources/features",
    glue       = {"com.saucedemo.steps", "com.saucedemo.support"},
    tags       = "not @wip",
    plugin     = {
        "pretty",
        "html:target/cucumber-reports/cucumber.html",
        "json:target/cucumber-reports/cucumber.json",
        "com.aventstack.extentreports.cucumber.adapter.ExtentCucumberAdapter:"
    },
    monochrome = true,       // cleaner console output without ANSI codes
    publish    = false       // disable Cucumber cloud publishing
)
public class TestRunner {
    // This class is intentionally empty.
    // JUnit + Cucumber framework handles everything via annotations.
}
