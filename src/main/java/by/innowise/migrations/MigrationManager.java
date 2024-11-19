package by.innowise.migrations;

import by.innowise.MigrationTool;
import by.innowise.db.ConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public class MigrationManager {

    private static final Logger logger = LoggerFactory.getLogger(MigrationManager.class);

    public static void migrate() {
        try (Connection connection = ConnectionManager.getConnection()) {
            connection.setAutoCommit(false);
            ensureHistoryTableExists(connection);
            List<File> migrationFiles = MigrationFileReader.getMigrationFiles("migrations/");
            for (File file : migrationFiles) {
                if (!isMigrationApplied(connection, file)) {
                    applyMigration(connection, file);
                }
            }
            connection.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean isMigrationApplied(Connection connection, File file) throws SQLException {
        String version = MigrationFileReader.extractVersion(file);
        String query = "SELECT COUNT(*) FROM migration_history WHERE version = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, version);
            ResultSet rs = ps.executeQuery();
            rs.next();
            return rs.getInt(1) > 0;
        }
    }

    private static void applyMigration(Connection connection, File file) throws SQLException, IOException {
        String sql = MigrationFileReader.readSQL(file);
        long startTime = System.currentTimeMillis();
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            long executionTime = System.currentTimeMillis() - startTime;
            String insertHistorySQL = """
                INSERT INTO migration_history (version, description, script, checksum, execution_time, success)
                VALUES (?, ?, ?, ?, ?, ?);
            """;
            try (PreparedStatement ps = connection.prepareStatement(insertHistorySQL)) {
                ps.setString(1, MigrationFileReader.extractVersion(file));
                ps.setString(2, "Migration " + file.getName());
                ps.setString(3, file.getName());
                ps.setInt(4, sql.hashCode());
                ps.setLong(5, executionTime);
                ps.setBoolean(6, true);
                ps.executeUpdate();
            }
        }
    }

    private static void ensureHistoryTableExists(Connection connection) throws SQLException {
        String createTableSQL = """
        CREATE TABLE IF NOT EXISTS migration_history (
            id SERIAL PRIMARY KEY,
            version VARCHAR(50) NOT NULL UNIQUE,
            description VARCHAR(255),
            script VARCHAR(255) NOT NULL,
            checksum INT NOT NULL,
            execution_time BIGINT NOT NULL,
            success BOOLEAN NOT NULL,
            applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        );
    """;

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createTableSQL);
            logger.info("Таблица 'migration_history' проверена или успешно создана.");
        } catch (SQLException e) {
            logger.error("Ошибка при создании таблицы 'migration_history'.", e);
            throw e;
        }
    }


    public static void rollback() {
        // Реализация отката
    }

    public static void status() {
        // Реализация статуса
    }
}
