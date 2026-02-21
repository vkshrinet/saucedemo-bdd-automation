# SauceDemo BDD Automation Framework

**Author:** Vivek Kumar Singh  
**Role Applied For:** QA Automation Engineer  
**Target App:** https://www.saucedemo.com  
**Stack:** Java 11 · Selenium 4 · Cucumber 7 · TestNG · Maven · MySQL

---

## Table of Contents
1. [Project Structure](#project-structure)
2. [Setup & Installation](#setup--installation)
3. [How to Run Tests](#how-to-run-tests)
4. [Locator Strategy](#locator-strategy-for-sauce-labs-backpack)
5. [Database Integration Design](#database-testing-integration)
6. [Engineering Notes](#engineering-notes)
7. [AI Usage Disclosure](#ai--tool-usage-disclosure)
8. [Assumptions](#assumptions)

---

## Project Structure

```
saucedemo-bdd/
├── features/                        # Gherkin .feature files ONLY
│   ├── purchase.feature             # Happy-path E2E purchase scenario
│   └── login.feature                # Negative login scenarios
│
├── steps/                           # Step definitions — NO locators here
│   ├── PurchaseSteps.java
│   └── LoginSteps.java
│
├── pages/                           # Page Objects: locators + page actions
│   ├── LoginPage.java
│   ├── InventoryPage.java
│   ├── CartPage.java
│   ├── CheckoutStepOnePage.java
│   ├── CheckoutStepTwoPage.java
│   └── OrderConfirmationPage.java
│
├── support/                         # Utilities and framework infrastructure
│   ├── DriverFactory.java           # ThreadLocal WebDriver creation/teardown
│   ├── Hooks.java                   # Cucumber @Before / @After
│   ├── WaitUtils.java               # Explicit wait helpers (zero Thread.sleep)
│   ├── DBConnectionUtil.java        # Config-driven DB connection + queries
│   ├── ConfigManager.java           # Env-based config loader (priority chain)
│   ├── TestDataLoader.java          # JSON fixture reader
│   ├── TestRunner.java              # JUnit + CucumberOptions
│   └── logback.xml                  # Logging configuration
│
├── config/
│   ├── qa.properties                # QA environment settings
│   ├── staging.properties           # (Template) Staging environment
│   └── .env.example                 # Environment variable template — NO real creds
│
├── testdata/
│   ├── users.json                   # User credentials (standard, locked, problem…)
│   └── checkout.json                # Customer details (valid, edge cases…)
│
├── .github/workflows/ci.yml         # GitHub Actions pipeline (Chrome + Firefox)
├── pom.xml                          # Maven dependencies and Surefire config
└── README.md
```

---

## Setup & Installation

### Prerequisites
- Java 11+
- Maven 3.8+
- Chrome or Firefox installed
- Git

### 1. Clone the repository
```bash
git clone https://github.com/vkshrinet/saucedemo-bdd-automation.git
cd saucedemo-bdd
```

### 2. Install dependencies
```bash
mvn clean install -DskipTests
```
> WebDriverManager automatically downloads the correct browser driver binary.  
> No manual ChromeDriver installation needed.

### 3. Configure environment (optional)
```bash
cp config/.env.example config/.env
# Edit .env with your DB credentials if running DB validation
```

---

## How to Run Tests

### Run all tests (Chrome, headed)
```bash
mvn test
```

### Run only @smoke tests
```bash
mvn test -Dcucumber.filter.tags="@smoke"
```

### Run with Firefox in headless mode
```bash
mvn test -Dbrowser=firefox -Dheadless=true
```

### Run with specific environment config
```bash
mvn test -Denv=staging
```

### Run with all overrides (CI-style)
```bash
mvn test -Dbrowser=chrome -Dheadless=true -Denv=qa -Dcucumber.filter.tags="@e2e"
```

### How to change browser / headless mode
- **Command line:** `-Dbrowser=firefox` or `-Dbrowser=edge`
- **Config file:** Edit `browser=chrome` in `config/qa.properties`
- **Environment variable:** `BROWSER=firefox mvn test`

Priority: system property > env variable > config file > default (chrome)

### View reports
After running:
```
target/cucumber-reports/cucumber.html   ← Cucumber HTML report
target/cucumber-reports/cucumber.json   ← JSON for CI parsing
target/reports/ExtentReport.html        ← ExtentReports dashboard
target/logs/test-run.log               ← Full execution log
```

---

## Locator Strategy for "Sauce Labs Backpack"

### Primary Strategy — Text-based XPath with data-test ancestor traversal

```java
By.xpath(
  "//div[@data-test='inventory-item-name' and text()='Sauce Labs Backpack']" +
  "/ancestor::div[@data-test='inventory-item']" +
  "//button[contains(@data-test,'add-to-cart')]"
)
```

**How it works:**
1. Finds the product name `<div>` that contains exactly "Sauce Labs Backpack"
2. Traverses UP to the parent inventory-item container using `ancestor::`
3. Traverses DOWN to the Add-to-Cart button scoped within that container

**Why this is robust:**
- Finds the product by its displayed NAME — the most human-readable identifier
- Not affected by product list order changing (no index-based selection)
- `data-test` attributes are explicitly added for automation stability — they don't change with CSS refactors
- Supports parameterised tests: just pass any product name as a variable
- Works even if more products are added to the catalogue

### Fallback Strategy — Direct data-test attribute on the button

```java
By.cssSelector("[data-test='add-to-cart-sauce-labs-backpack']")
```

**Why it's acceptable but less ideal:**
- Simple, fast, readable
- Stable as long as the product URL slug doesn't change
- BUT: the slug is derived from the product name — if the name changes, the slug changes
- NOT parameterisable: each product needs its own hardcoded selector
- Better for a single-product smoke test; worse for a data-driven framework

### What was avoided and why

| Avoided Locator | Problem |
|---|---|
| `(//button[@class='btn_inventory'])[1]` | Index-based — breaks if product order changes |
| `//div[@class='inventory_item'][1]//button` | Index + class — breaks on CSS refactor |
| `.btn_inventory:nth-child(1)` | CSS child index — same problem |
| Absolute XPath `/html/body/div[1]/div/...` | Completely brittle, breaks on any DOM change |

---

## Database Testing Integration

> **Note:** SauceDemo is a frontend-only demo app with no real database.  
> This section demonstrates the integration pattern I would use in a production project.

### DB Connection Utility (`support/DBConnectionUtil.java`)

- Config-driven: DB URL, username, password come from `config/{env}.properties` or environment variables — never hardcoded
- All queries use `PreparedStatement` (prevents SQL injection in test data)
- `try-with-resources` guarantees connection teardown even on assertion failures
- Exposes `executeQuery()` returning `List<Map<String, String>>` — flexible for any column structure

### DB Validation Pseudocode (Post-Purchase Validation)

```java
// In step definition, after "I complete the order":
@And("the order should be recorded in the database")
public void orderShouldBeRecordedInDB() {
    String sessionId = SessionContext.getSessionId();  // captured during login

    // SQL query — validates the order record exists with correct status
    String sql = "SELECT status, total_amount, user_id " +
                 "FROM orders WHERE session_id = ? AND status = 'completed'";

    // Retry up to 5 times with 2s interval (eventual consistency)
    boolean recordFound = DBConnectionUtil.waitForRecord(
        "SELECT COUNT(*) FROM orders WHERE session_id = ? AND status = 'completed'",
        1,           // expected row count
        5,           // max attempts
        2000,        // interval ms
        sessionId
    );

    assertTrue("Order record should appear in DB within 10 seconds", recordFound);

    // Validate specific columns
    List<Map<String,String>> rows = DBConnectionUtil.executeQuery(sql, sessionId);
    assertEquals("completed", rows.get(0).get("status"));
    assertNotNull(rows.get(0).get("total_amount"));
}

// Cleanup in @After hook — test isolation
@After
public void cleanupDB(Scenario scenario) {
    if (sessionId != null) {
        DBConnectionUtil.cleanupTestData(
            "DELETE FROM orders WHERE session_id = ?", sessionId
        );
    }
    DriverFactory.quitDriver();
}
```

### Isolation Strategy
- Each test scenario generates a unique `session_id` or `test_run_id`
- All DB assertions and cleanups are scoped to that ID
- No shared state between scenarios — tests can run in parallel safely

### Handling Eventual Consistency
- `DBConnectionUtil.waitForRecord()` wraps `WaitUtils.retryUntil()`
- Retries the COUNT query up to N times with a configurable interval
- Logs each attempt — failure messages show exactly how many retries occurred
- The `Thread.sleep()` inside `retryUntil()` is documented and justified: it's polling a DB, not a UI element

---

## Engineering Notes

### 1. Why did you choose this framework structure?

Each folder has a single, clear responsibility that mirrors the BDD principle of separation of concerns:

- **`/features`** contains only Gherkin — non-technical stakeholders can read and contribute scenarios without touching Java
- **`/steps`** orchestrates the test flow by calling page and utility methods — it reads like plain English
- **`/pages`** is the only place that knows about the DOM — change a locator here and it fixes every test using it
- **`/support`** houses infrastructure (driver, waits, DB, config) — business logic and framework plumbing never mix
- **`/testdata`** externalises all test inputs — you can add a new user or checkout scenario without modifying a single Java file
- **`/config`** enables environment switching without code changes — `mvn test -Denv=staging` just works

This structure directly enables scalability: adding a new feature means adding a `.feature` file, step class, and page class — never modifying existing ones.

---

### 2. How does your wait strategy prevent flakiness?

**Zero Thread.sleep() in production test code.**

Every UI interaction goes through `WaitUtils`:
- `waitForVisible()` — ensures the element is rendered and has non-zero size before interacting
- `waitForClickable()` — goes one step further: ensures the element is also enabled (prevents `ElementNotInteractableException` on async-rendered buttons)
- `waitForText()` — waits for cart badge count to update rather than reading it immediately after a click
- `waitForUrlContains()` — used for page navigation validation instead of checking elements that might appear on multiple pages

`WebDriverWait` polls the DOM every 500ms and throws `TimeoutException` with a clear message if the condition isn't met in the configured time.

The one justified `Thread.sleep()` is inside `retryUntil()` for DB polling — a DB call is external and condition-based, not DOM-based, so Selenium's `ExpectedConditions` don't apply.

---

### 3. How does your locator strategy improve stability?

Three principles applied:

**a) Prefer `data-test` attributes**  
The SauceDemo app ships with `data-test="login-button"`, `data-test="inventory-item-name"` etc. These are stable by design — they exist for testing and won't change with CSS refactors or layout changes.

**b) Centralize all locators in Page Objects**  
Every locator is a `private static final By` constant in one class. When the app changes, one line changes, all tests using it are fixed.

**c) Avoid index-based and brittle selectors**  
The Backpack locator finds by text then traverses to the sibling button — no index, no class dependency. Adding 10 more products to the catalogue won't break the test.

---

### 4. How would you scale this to 50+ scenarios?

**Structural:**
- Add feature files per domain: `cart.feature`, `filters.feature`, `checkout_edge_cases.feature`
- Create shared step libraries for common flows (login, logout) reused across feature files
- Use `@DataProvider`-style `Scenario Outline` for data-driven coverage without duplicating steps

**Execution:**
- `TestRunner` already uses `tags` — split into `@smoke` (5 scenarios, 2 min), `@regression` (50 scenarios, parallel), `@nightly` (all + DB validation)
- `DriverFactory` uses `ThreadLocal` — parallel execution works out of the box. Configure in `maven-surefire-plugin` with `<parallel>methods</parallel>`

**Maintenance:**
- `ConfigManager`'s priority chain means adding a staging environment is just a new `staging.properties` file
- `TestDataLoader` means adding test users is a JSON change, not a code change
- Page Objects are scoped per page — a redesign of the cart page only touches `CartPage.java`

**Reporting:**
- ExtentReports adapter generates a dashboard with screenshots, timings, pass/fail breakdown
- GitHub Actions publishes reports as artefacts and test summaries on every PR

---

### 5. How would you execute this in CI/CD?

The included `.github/workflows/ci.yml` demonstrates the complete pipeline:

1. **Trigger:** on push to main/develop, on PR, and nightly cron
2. **Matrix:** runs Chrome + Firefox in parallel
3. **Setup:** Java 11, Maven cache, browser installation
4. **Execution:** `mvn test -Dbrowser=${{ matrix.browser }} -Dheadless=true`
5. **Credentials:** DB password injected from GitHub Secrets — never in source
6. **Artefacts:** Reports uploaded regardless of pass/fail; screenshots uploaded on failure only
7. **Jenkins equivalent:** `mvn test` in a Pipeline stage, `junit '**/cucumber.json'` for test results, and `publishHTML` for the Extent report

---

### 6. Two improvements I would make with more time

**1. Retry failed scenarios automatically**  
Transient network issues or browser startup hiccups occasionally fail a healthy test. I'd add the `cucumber-jvm-retry` plugin (or a custom `RetryAnalyzer`) to re-run failed scenarios once before marking them as failed. This separates true product bugs from infrastructure noise — critical for a nightly regression suite.

**2. API-layer test setup**  
Currently, each scenario drives through the login UI before the actual test begins. In a real app with a REST API, I'd seed the pre-conditions (authenticated session, cart state) via API calls in `@Before`, then start the Selenium test at the meaningful step. This cuts scenario runtime by 30–50% and removes UI flakiness from setup steps that aren't under test.

---

## AI / Tool Usage Disclosure

As required by the assignment's AI usage policy:

**AI tools used:** Claude (Anthropic) was used as a pair-programming assistant.

**What was AI-assisted:**
- Scaffolding boilerplate structure (pom.xml dependencies, logback.xml)
- Drafting initial versions of JavaDoc comments and README sections
- Suggesting the `retryUntil()` pattern for eventual consistency

**What I wrote and why I kept/modified it:**
- All locator strategy decisions are my own, based on 4+ years of Selenium experience with BATON and ORION projects at Interra Systems
- The ThreadLocal `DriverFactory` pattern is one I've used in production for parallel execution
- The `ConfigManager` priority chain (system prop → env var → file → default) is a pattern I independently designed and verified
- I reviewed every generated line, removed unused imports, corrected package names, and validated logic against Selenium 4 API changes

**I can explain and modify every line of this codebase in the 15-minute technical discussion.**

---

## Assumptions

1. **No real DB access:** SauceDemo has no backend DB. The `DBConnectionUtil` demonstrates design pattern only; pseudocode shows real integration.
2. **Chrome as default:** Tested on Chrome 120+. Firefox support included via `DriverFactory`.
3. **Java 11:** Used for `switch` expression syntax. Java 8 users would replace with `if/else`.
4. **Test data stability:** SauceDemo's user credentials (`standard_user` / `secret_sauce`) are public and documented on the site — hardcoding them in `users.json` is intentional and appropriate here.
5. **Single-window tests:** No multi-tab handling needed for the specified scenarios.
6. **Extent Reports adapter:** Requires a `extent.properties` file in `src/test/resources` — included in full repository.
