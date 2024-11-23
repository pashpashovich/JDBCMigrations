package by.innowise.migrations;

import by.innowise.db.PropertiesUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


class MigrationManagerIntegrationTest {

    private static final String CREATE_TABLE_IF_NOT_EXISTS = """
                CREATE TABLE IF NOT EXISTS migration_history (
                    id SERIAL PRIMARY KEY,
                    version VARCHAR(50) NOT NULL UNIQUE,
                    description VARCHAR(255),
                    script VARCHAR(255) NOT NULL,
                    checksum INT NOT NULL,
                    execution_time BIGINT NOT NULL,
                    success BOOLEAN NOT NULL,
                    reverted BOOLEAN DEFAULT FALSE,
                    applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                );
            """;
    private static final String QUERY = "SELECT COUNT(*) FROM migration_history";
    private static final String QUERY1 = "SELECT COUNT(*) FROM migration_history WHERE reverted = TRUE";
    private static final String QUERY2 = "SELECT COUNT(*) FROM migration_history WHERE reverted = TRUE";
    private static final String QUERY3 = "SELECT COUNT(*) FROM migration_history WHERE reverted = TRUE";
    private static final String QUERY4 = "SELECT COUNT(*) FROM migration_history WHERE reverted = TRUE";
    private static final String QUERY5 = "SELECT COUNT(*) FROM migration_history";
    private static final String DROP_SCHEMA_PUBLIC_CASCADE_CREATE_SCHEMA_PUBLIC = "DROP SCHEMA public CASCADE; CREATE SCHEMA public;";
    private Connection connection;

    @BeforeEach
    void setUp() throws Exception {
        System.setProperty("config.file", "application-test.properties");
        String url = PropertiesUtils.getProperty("db.url");
        String user = PropertiesUtils.getProperty("db.username");
        String password = PropertiesUtils.getProperty("db.password");

        connection = DriverManager.getConnection(url, user, password);

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(CREATE_TABLE_IF_NOT_EXISTS);
        }
    }

    @Test
    void shouldApplyMigration() throws Exception {
        //given,when
        MigrationManager.migrate();
        //then
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(QUERY)) {
            assertTrue(rs.next());
            assertEquals(3, rs.getInt(1));
        }
    }

    @Test
    void shouldRollbackToTag() throws Exception {
        // given
        MigrationManager.migrate();
        //when
        MigrationManager.rollbackToTag("1");
        //then
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(QUERY1)) {
            assertTrue(rs.next());
            assertEquals(2, rs.getInt(1));
        }
    }

    @Test
    void shouldRollbackToDate() throws Exception {
        // given
        MigrationManager.migrate();
        String rollbackDate = "2024-11-20 00:00:00";
        //when
        MigrationManager.rollbackToDate(rollbackDate);
        //then
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(QUERY2)) {
            assertTrue(rs.next());
            assertEquals(3, rs.getInt(1));
        }
    }

    @Test
    void shouldNotRollbackToDate() throws Exception {
        // given
        MigrationManager.migrate();
        String rollbackDate = "2100-11-20 00:00:00";
        //when
        MigrationManager.rollbackToDate(rollbackDate);
        //then
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(QUERY2)) {
            assertTrue(rs.next());
            assertEquals(0, rs.getInt(1));
        }
    }

    @Test
    void shouldRollbackCount() throws Exception {
        // given
        MigrationManager.migrate();
        //when
        MigrationManager.rollbackCount(1);
        //then
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(QUERY3)) {
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1));
        }
    }

    @Test
    void shouldNotRollbackIfNoMigrations() throws Exception {
        // given
        //when
        MigrationManager.rollbackToTag("1");
        //then
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(QUERY4)) {
            assertTrue(rs.next());
            assertEquals(0, rs.getInt(1));
        }
    }

    @Test
    void shouldShowInfo() throws Exception {
        // given
        MigrationManager.migrate();
        //when
        //then
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(QUERY5)) {
            assertTrue(rs.next());
            assertEquals(3, rs.getInt(1));
        }
        assertDoesNotThrow(MigrationManager::info);
    }

    @AfterEach
    void tearDown() throws Exception {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(DROP_SCHEMA_PUBLIC_CASCADE_CREATE_SCHEMA_PUBLIC);
        }
        connection.close();
    }
}
