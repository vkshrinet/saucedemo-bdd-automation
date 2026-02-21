package com.saucedemo.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * ConfigManager — single source of truth for all configuration.
 *
 * Priority order (highest → lowest):
 *   1. System properties  (-Dbrowser=firefox)
 *   2. Environment variables (CI_BROWSER=firefox)
 *   3. config/{env}.properties file
 *   4. Supplied default value
 *
 * This design means CI/CD pipelines can override any setting without
 * modifying source files.
 */
public class ConfigManager {

    private static final Logger log = LoggerFactory.getLogger(ConfigManager.class);
    private static final Properties props = new Properties();

    static {
        String env = System.getProperty("env", "qa");
        String configFile = "config/" + env + ".properties";
        log.info("Loading config from: {}", configFile);

        try (InputStream is = ConfigManager.class.getClassLoader().getResourceAsStream(configFile)) {
            if (is != null) {
                props.load(is);
                log.info("Config loaded successfully for env: {}", env);
            } else {
                log.warn("Config file not found: {}. Falling back to defaults.", configFile);
            }
        } catch (IOException e) {
            log.error("Failed to load config: {}", e.getMessage());
        }
    }

    private ConfigManager() { }

    /**
     * Returns a config value by key, with a fallback default.
     * System properties and env vars take priority over the properties file.
     */
    public static String get(String key, String defaultValue) {
        // 1. System property
        String value = System.getProperty(key);
        if (value != null) return value;

        // 2. Environment variable (dots → underscores, uppercase)
        String envKey = key.replace(".", "_").toUpperCase();
        value = System.getenv(envKey);
        if (value != null) return value;

        // 3. Properties file
        value = props.getProperty(key);
        if (value != null) return value;

        // 4. Default
        log.debug("Key '{}' not found in config. Using default: '{}'", key, defaultValue);
        return defaultValue;
    }

    public static String get(String key) {
        return get(key, null);
    }
}
