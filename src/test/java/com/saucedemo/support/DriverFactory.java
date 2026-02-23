package com.saucedemo.support;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Duration;

/**
 * DriverFactory — centralizes WebDriver creation and teardown.
 *
 * ThreadLocal ensures each thread gets its own driver instance,
 * enabling safe parallel execution without shared state.
 * WebDriverManager eliminates manual driver binary management.
 */
public class DriverFactory {

    private static final Logger log = LoggerFactory.getLogger(DriverFactory.class);
    private static final ThreadLocal<WebDriver> driverThreadLocal = new ThreadLocal<>();
    private static final int TIMEOUT_SECONDS = 10;

    private DriverFactory() { }

    /**
     * Initialises a WebDriver for the requested browser.
     * Call once per scenario in the @Before hook.
     */
    public static void initDriver() {
        String browser = System.getProperty("browser",
                ConfigManager.get("browser", "chrome"));
        boolean headless = Boolean.parseBoolean(
                System.getProperty("headless",
                        ConfigManager.get("headless", "false")));

        log.info("Initialising '{}' driver | headless={}", browser, headless);

        WebDriver driver;

        if ("firefox".equalsIgnoreCase(browser)) {
            driver = createFirefoxDriver(headless);
        } else if ("edge".equalsIgnoreCase(browser)) {
            driver = createEdgeDriver(headless);
        } else {
            driver = createChromeDriver(headless);  // chrome is the default
        }

        driver.manage().window().maximize();
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(TIMEOUT_SECONDS));
        driverThreadLocal.set(driver);
        log.info("Driver initialised: {}", driver.getClass().getSimpleName());
    }

    /**
     * Returns the WebDriver for the current thread.
     */
    public static WebDriver getDriver() {
        WebDriver driver = driverThreadLocal.get();
        if (driver == null) {
            throw new IllegalStateException(
                "WebDriver not initialised. Did you call DriverFactory.initDriver()?");
        }
        return driver;
    }

    /**
     * Quits the driver and removes it from ThreadLocal.
     * Call in the @After hook to prevent memory leaks.
     */
    public static void quitDriver() {
        WebDriver driver = driverThreadLocal.get();
        if (driver != null) {
            try {
                driver.quit();
                log.info("Driver quit successfully.");
            } catch (Exception e) {
                log.warn("Exception while quitting driver: {}", e.getMessage());
            } finally {
                driverThreadLocal.remove();
            }
        }
    }

    // ── Private factory methods ───────────────────────────────────────────────

    /**
     * Create Chrome WebDriver with appropriate options for CI/CD and local environments
     */
    private static WebDriver createChromeDriver(boolean headless) {
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        
        if (headless) {
            options.addArguments("--headless=new");
        }
        
        // Essential Chrome arguments for CI/CD environments
        options.addArguments(
            "--no-sandbox",                    // Required for CI/CD
            "--disable-dev-shm-usage",         // Use /tmp instead of /dev/shm (limited in containers)
            "--disable-gpu",                   // Disable GPU acceleration
            "--disable-software-rasterizer",
            "--disable-extensions",
            "--window-size=1920,1080",
            "--start-maximized",
            "--disable-blink-features=AutomationControlled"
        );
        
        options.setAcceptInsecureCerts(true);
        
        log.info("Chrome options configured for headless={}", headless);
        return new ChromeDriver(options);
    }

    /**
     * Create Firefox WebDriver with proper headless mode support
     * 
     * Firefox in headless mode requires special configuration:
     * - Browser preferences must be set for headless stability
     * - Marionette protocol must be enabled
     * - CI/CD environment arguments must be configured
     */
    private static WebDriver createFirefoxDriver(boolean headless) {
        WebDriverManager.firefoxdriver().setup();
        FirefoxOptions options = new FirefoxOptions();
        
        if (headless) {
            options.addArguments("--headless");
        }
        
        // Essential Firefox arguments for CI/CD environments
        options.addArguments(
            "--no-sandbox",                    // Required for containers
            "--disable-dev-shm-usage"          // Use /tmp instead of /dev/shm
        );
        
        options.setAcceptInsecureCerts(true);
        
        log.info("Firefox options configured for headless={}", headless);
        return new FirefoxDriver(options);
    }

    /**
     * Create Edge WebDriver with appropriate options
     */
    private static WebDriver createEdgeDriver(boolean headless) {
        WebDriverManager.edgedriver().setup();
        EdgeOptions options = new EdgeOptions();
        
        if (headless) {
            options.addArguments("--headless=new");
        }
        
        // Essential Edge arguments for CI/CD environments
        options.addArguments(
            "--no-sandbox",
            "--disable-dev-shm-usage",
            "--disable-gpu"
        );
        
        options.setAcceptInsecureCerts(true);
        
        log.info("Edge options configured for headless={}", headless);
        return new EdgeDriver(options);
    }
}
