package com.saucedemo.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

/**
 * TestDataLoader — reads fixture JSON files from /testdata directory.
 *
 * Design decision: All test data lives in JSON files, NOT in step definitions
 * or page objects. This keeps tests environment-agnostic and makes data changes
 * trivial (no recompilation needed).
 *
 * Usage:
 *   JsonNode user = TestDataLoader.get("users.json", "standard_user");
 *   String username = user.get("username").asText();
 */
public class TestDataLoader {

    private static final Logger log = LoggerFactory.getLogger(TestDataLoader.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private TestDataLoader() { }

    /**
     * Loads a JSON file from the testdata/ directory and returns a named node.
     *
     * @param fileName  e.g. "users.json"
     * @param key       top-level key inside the JSON object
     */
    public static JsonNode get(String fileName, String key) {
        String path = "testdata/" + fileName;
        log.debug("Loading test data: {} → key: {}", path, key);

        try (InputStream is = TestDataLoader.class.getClassLoader().getResourceAsStream(path)) {
            if (is == null) {
                throw new RuntimeException("Test data file not found on classpath: " + path);
            }
            JsonNode root = mapper.readTree(is);
            JsonNode node = root.get(key);
            if (node == null) {
                throw new RuntimeException("Key '" + key + "' not found in " + fileName);
            }
            return node;
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse test data: " + path, e);
        }
    }

    /** Convenience method — reads the whole file as a JsonNode. */
    public static JsonNode load(String fileName) {
        String path = "testdata/" + fileName;
        try (InputStream is = TestDataLoader.class.getClassLoader().getResourceAsStream(path)) {
            if (is == null) throw new RuntimeException("File not found: " + path);
            return mapper.readTree(is);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load: " + path, e);
        }
    }
}
