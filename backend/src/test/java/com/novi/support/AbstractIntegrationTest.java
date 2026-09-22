package com.novi.support;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base class for tests that need a real database. Tests exercise the real
 * Flyway migrations and SQL constraints against a real PostgreSQL instance
 * rather than an in-memory approximation.
 *
 * <p>By default this spins up a throwaway PostgreSQL instance via Testcontainers.
 * In environments where the JVM can't drive Testcontainers directly (for
 * example, running the build inside a Linux container against a Windows Docker
 * engine), point the tests at an already-running PostgreSQL by setting the
 * {@code IT_DB_URL} / {@code IT_DB_USERNAME} / {@code IT_DB_PASSWORD}
 * environment variables; when {@code IT_DB_URL} is present, Testcontainers is
 * skipped entirely.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    private static final String EXTERNAL_DB_URL = System.getenv("IT_DB_URL");

    private static final PostgreSQLContainer<?> POSTGRES;

    static {
        if (EXTERNAL_DB_URL == null || EXTERNAL_DB_URL.isBlank()) {
            POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("novi_test")
                    .withUsername("novi")
                    .withPassword("novi");
            POSTGRES.start();
        } else {
            POSTGRES = null;
        }
    }

    @Autowired
    protected TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * These are HTTP-level integration tests, so there's no transactional
     * rollback between them. Reset the per-user data before each test so the
     * suite is deterministic - both under Testcontainers (fresh DB per run) and
     * when pointed at a reused external database. TRUNCATE ... CASCADE clears
     * the dependent rows (user_books, ratings, reviews, reading_history,
     * shelves, recommendations, scans); the shared book/author catalog is left
     * intact.
     */
    @BeforeEach
    void resetUserData() {
        jdbcTemplate.execute("TRUNCATE TABLE users RESTART IDENTITY CASCADE");
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        if (POSTGRES != null) {
            registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
            registry.add("spring.datasource.username", POSTGRES::getUsername);
            registry.add("spring.datasource.password", POSTGRES::getPassword);
        } else {
            registry.add("spring.datasource.url", () -> EXTERNAL_DB_URL);
            registry.add("spring.datasource.username", () -> envOrDefault("IT_DB_USERNAME", "novi"));
            registry.add("spring.datasource.password", () -> envOrDefault("IT_DB_PASSWORD", "novi"));
        }
    }

    private static String envOrDefault(String key, String fallback) {
        String value = System.getenv(key);
        return (value == null || value.isBlank()) ? fallback : value;
    }
}
