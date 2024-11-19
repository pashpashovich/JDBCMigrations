package by.innowise.migrations;

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

/**
 * Класс для работы с примененными и новыми миграциями
 */
public class MigrationManager {

    private static final Logger logger = LoggerFactory.getLogger(MigrationManager.class);
    public static final String DIRECTORY_PATH = "migrations/";

    private MigrationManager() {
    }

    /**
     * Метод, который выполняет не примененные миграции к БД
     */
    public static void migrate() {
        try (Connection connection = ConnectionManager.getConnection()) {
            connection.setAutoCommit(false);
            ensureHistoryTableExists(connection);
            List<File> migrationFiles = MigrationFileReader.getMigrationFiles(DIRECTORY_PATH);
            for (File file : migrationFiles) {
                if (!isMigrationApplied(connection, file)) {
                    applyMigration(connection, file);
                }
            }
            connection.commit();
        } catch (Exception e) {
            logger.error("Ошибка во время миграции.", e);
        }
    }

    /**
     * Метод, который откатывает БД к состоянию с версией tag
     *
     * @param tag - версия БД
     */
    public static void rollbackToTag(String tag) {
        try (Connection connection = ConnectionManager.getConnection()) {
            connection.setAutoCommit(false);
            clearDatabase(connection);
            ensureHistoryTableExists(connection);
            List<File> migrationFiles = MigrationFileReader.getMigrationFiles(DIRECTORY_PATH);
            for (File file : migrationFiles) {
                String version = MigrationFileReader.extractVersion(file);
                if (version.compareTo(tag) > 0) {
                    logger.info("Достигнута указанная версия {}. Остановка выполнения миграций.", tag);
                    break;
                }
                applyMigration(connection, file);
            }
            markMigrationsAsRevertedAfterTag(connection, tag);
            connection.commit();
            logger.info("Откат до версии {} успешно выполнен.", tag);
        } catch (Exception e) {
            logger.error("Ошибка при выполнении отката до версии.", e);
        }
    }


    /**
     * Метод, который откатывает БД к дате date
     *
     * @param date - дата, к которой необходимо откатить БД
     */
    public static void rollbackToDate(String date) {
        try (Connection connection = ConnectionManager.getConnection()) {
            connection.setAutoCommit(false);
            clearDatabase(connection);
            ensureHistoryTableExists(connection);
            List<File> migrationFiles = MigrationFileReader.getMigrationFiles(DIRECTORY_PATH);
            for (File file : migrationFiles) {
                applyMigration(connection, file);
            }
            markMigrationsAsRevertedAfterDate(connection, date);
            connection.commit();
            logger.info("Откат миграций до даты {} успешно выполнен.", date);
        } catch (Exception e) {
            logger.error("Ошибка при выполнении отката до даты.", e);
        }
    }

    /**
     * Метод, выводящий информацию о текущем состоянии БД, включая версию базы данных и список всех миграций
     */
    public static void info() {
        try (Connection connection = ConnectionManager.getConnection()) {
            logger.info("Получение статуса базы данных...");
            String currentVersionQuery = """
                        SELECT version FROM migration_history
                        WHERE reverted = FALSE
                        ORDER BY version DESC LIMIT 1
                    """;
            try (PreparedStatement ps = connection.prepareStatement(currentVersionQuery)) {
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    String currentVersion = rs.getString("version");
                    logger.info("Актуальная версия базы данных: {}", currentVersion);
                } else {
                    logger.info("Миграции не применялись. База данных находится в начальном состоянии.");
                }
            }
            String allMigrationsQuery = """
                        SELECT version, description, applied_at, reverted FROM migration_history
                        WHERE reverted = FALSE
                        ORDER BY applied_at
                    """;
            try (PreparedStatement ps = connection.prepareStatement(allMigrationsQuery)) {
                ResultSet rs = ps.executeQuery();
                logger.info("Список миграций:");
                while (rs.next()) {
                    String version = rs.getString("version");
                    String description = rs.getString("description");
                    String appliedAt = rs.getTimestamp("applied_at").toString();
                    boolean reverted = rs.getBoolean("reverted");

                    logger.info("  - Версия: {}, Описание: {}, Применена: {}, Откатана: {}",
                            version, description, appliedAt, reverted ? "Да" : "Нет");
                }
            }
        } catch (SQLException e) {
            logger.error("Ошибка при получении статуса базы данных.", e);
        }
    }


    private static boolean isMigrationApplied(Connection connection, File file) throws SQLException {
        String version = MigrationFileReader.extractVersion(file);
        String query = "SELECT COUNT(*) FROM migration_history WHERE version = ? AND reverted = FALSE";
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
        MigrationExecutor.executeQuery(connection, sql);
        long executionTime = System.currentTimeMillis() - startTime;
        String insertHistorySQL = """
                    INSERT INTO migration_history (version, description, script, checksum, execution_time, success, reverted)
                    VALUES (?, ?, ?, ?, ?, ?, FALSE)
                    ON CONFLICT (version) DO UPDATE SET
                    description = EXCLUDED.description,
                    script = EXCLUDED.script,
                    checksum = EXCLUDED.checksum,
                    execution_time = EXCLUDED.execution_time,
                    success = EXCLUDED.success,
                    reverted = FALSE;
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

    private static void ensureHistoryTableExists(Connection connection) {
        String createTableSQL = """
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
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createTableSQL);
            logger.info("Таблица 'migration_history' проверена или успешно создана.");
        } catch (SQLException e) {
            logger.error("Ошибка при создании таблицы 'migration_history'.", e);
        }
    }


    private static void markMigrationsAsRevertedAfterTag(Connection connection, String tag) throws SQLException {
        String updateQuery = "UPDATE migration_history SET reverted = TRUE WHERE version > ?";
        try (PreparedStatement ps = connection.prepareStatement(updateQuery)) {
            ps.setString(1, tag);
            ps.executeUpdate();
            logger.info("Миграции после версии {} помечены как откатанные.", tag);
        }
    }

    private static void markMigrationsAsRevertedAfterDate(Connection connection, String date) throws SQLException {
        String updateQuery = "UPDATE migration_history SET reverted = TRUE WHERE applied_at > ?";
        try (PreparedStatement ps = connection.prepareStatement(updateQuery)) {
            ps.setString(1, date);
            ps.executeUpdate();
            logger.info("Миграции, выполненные после даты {}, помечены как откатанные.", date);
        }
    }

    public static void rollbackCount(int count) {
        try (Connection connection = ConnectionManager.getConnection()) {
            connection.setAutoCommit(false);
            clearDatabase(connection);
            ensureHistoryTableExists(connection);
            List<File> migrationFiles = MigrationFileReader.getMigrationFiles(DIRECTORY_PATH);
            for (int i = 0; i < migrationFiles.size() - count; i++) {
                applyMigration(connection, migrationFiles.get(i));
            }
            markLastNReverted(connection, count);
            connection.commit();
            logger.info("Откат последних {} миграций успешно выполнен.", count);
        } catch (Exception e) {
            logger.error("Ошибка при выполнении отката последних миграций.", e);
        }
    }

    private static void markLastNReverted(Connection connection, int count) throws SQLException {
        String updateQuery = """
                    UPDATE migration_history SET reverted = TRUE
                    WHERE id IN (SELECT id FROM migration_history ORDER BY applied_at DESC LIMIT ?);
                """;
        try (PreparedStatement ps = connection.prepareStatement(updateQuery)) {
            ps.setInt(1, count);
            ps.executeUpdate();
            logger.info("Последние {} миграций помечены как откатанные.", count);
        }
    }

    private static void clearDatabase(Connection connection) {
        logger.info("Очистка базы данных...");
        try (Statement stmt = connection.createStatement()) {
            String dropTablesSQL = """
                        DO $$
                        DECLARE
                            r RECORD;
                        BEGIN
                            FOR r IN (SELECT tablename FROM pg_tables WHERE schemaname = 'public' AND tablename != 'migration_history') LOOP
                                EXECUTE 'DROP TABLE IF EXISTS ' || r.tablename || ' CASCADE';
                            END LOOP;
                        END $$;
                    """;
            stmt.execute(dropTablesSQL);
            logger.info("Все таблицы, кроме 'migration_history', успешно удалены.");
        } catch (SQLException e) {
            logger.error("Ошибка при очистке базы данных.", e);
        }
    }
}
