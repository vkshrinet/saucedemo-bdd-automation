package com.saucedemo.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DBConnectionUtil — config-driven database utility for test validation.
 *
 * Design decisions:
 *  - Connection details come exclusively from ConfigManager (environment-driven).
 *    No credentials are ever hardcoded.
 *  - Uses try-with-resources for all JDBC operations — guarantees clean teardown
 *    even if an assertion throws mid-query.
 *  - Exposes a generic executeQuery() that returns List<Map<>> so any
 *    step definition can validate arbitrary columns without SQL knowledge.
 *  - retryQuery() supports eventual consistency: the DB record may not appear
 *    immediately after a UI action completes.
 *
 * NOTE: SauceDemo is a frontend-only demo app with no real database.
 *       This utility demonstrates how DB validation WOULD be wired in a
 *       real project. The pseudocode in the README shows the full flow.
 */
public class DBConnectionUtil {

    private static final Logger log = LoggerFactory.getLogger(DBConnectionUtil.class);

    // Loaded lazily from environment config
    private static final String DB_URL      = ConfigManager.get("db.url",      "jdbc:mysql://localhost:3306/saucedemo_db");
    private static final String DB_USER     = ConfigManager.get("db.username", "qa_user");
    private static final String DB_PASSWORD = ConfigManager.get("db.password", "");  // inject via .env / CI secret

    private DBConnectionUtil() { }

    // ── Connection ───────────────────────────────────────────────────────────

    /**
     * Opens and returns a new JDBC connection.
     * Caller is responsible for closing it (use try-with-resources).
     */
    public static Connection getConnection() throws SQLException {
        log.info("Opening DB connection to: {}", DB_URL);
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    // ── Query execution ──────────────────────────────────────────────────────

    /**
     * Executes a SELECT query and returns results as a list of row-maps.
     * Each map key is a column name; value is the cell value as a String.
     *
     * Example:
     *   List<Map<String,String>> rows = DBConnectionUtil.executeQuery(
     *       "SELECT * FROM orders WHERE user_id = ?", userId);
     *   assertEquals("completed", rows.get(0).get("status"));
     */
    public static List<Map<String, String>> executeQuery(String sql, Object... params) {
        List<Map<String, String>> results = new ArrayList<>();
        log.debug("Executing query: {}", sql);

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // Bind parameters safely — prevents SQL injection
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                int cols = meta.getColumnCount();

                while (rs.next()) {
                    Map<String, String> row = new HashMap<>();
                    for (int i = 1; i <= cols; i++) {
                        row.put(meta.getColumnName(i), rs.getString(i));
                    }
                    results.add(row);
                }
            }

            log.info("Query returned {} row(s)", results.size());
        } catch (SQLException e) {
            log.error("DB query failed: {} | SQL: {}", e.getMessage(), sql);
            throw new RuntimeException("DB query execution failed", e);
        }

        return results;
    }

    /**
     * Executes an INSERT / UPDATE / DELETE and returns rows affected.
     * Used in test setup (seed data) and teardown (cleanup).
     */
    public static int executeUpdate(String sql, Object... params) {
        log.debug("Executing update: {}", sql);
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            int affected = stmt.executeUpdate();
            log.info("Update affected {} row(s)", affected);
            return affected;

        } catch (SQLException e) {
            log.error("DB update failed: {}", e.getMessage());
            throw new RuntimeException("DB update execution failed", e);
        }
    }

    // ── Eventual consistency ─────────────────────────────────────────────────

    /**
     * Retries a count query until the expected row count is found or timeout.
     *
     * Use case: after completing a UI purchase, the order record may take
     * a few seconds to appear in the DB (async processing, message queues).
     *
     * @param sql           COUNT query, e.g. "SELECT COUNT(*) FROM orders WHERE user_id = ?"
     * @param expectedCount number of rows expected
     * @param maxAttempts   how many times to retry before failing
     * @param intervalMs    milliseconds between retries
     * @param params        query parameters
     */
    public static boolean waitForRecord(String sql, int expectedCount,
                                        int maxAttempts, long intervalMs, Object... params) {
        log.info("Waiting for DB record | Expected rows: {} | Max attempts: {}", expectedCount, maxAttempts);

        return WaitUtils.retryUntil(() -> {
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                for (int i = 0; i < params.length; i++) {
                    stmt.setObject(i + 1, params[i]);
                }

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        int count = rs.getInt(1);
                        log.debug("Row count: {}", count);
                        return count == expectedCount;
                    }
                }
            } catch (SQLException e) {
                log.warn("Retry query failed: {}", e.getMessage());
            }
            return false;
        }, maxAttempts, intervalMs);
    }

    // ── Cleanup utility ──────────────────────────────────────────────────────

    /**
     * Deletes test-generated records after a scenario.
     * Call from @After hook for data isolation between test runs.
     *
     * Example:
     *   DBConnectionUtil.cleanupTestData("DELETE FROM orders WHERE session_id = ?", sessionId);
     */
    public static void cleanupTestData(String sql, Object... params) {
        log.info("Cleaning up test data: {}", sql);
        int deleted = executeUpdate(sql, params);
        log.info("Cleanup removed {} record(s)", deleted);
    }
}
