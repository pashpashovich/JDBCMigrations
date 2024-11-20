package by.innowise.migrations;

import by.innowise.db.ConnectionManager;
import by.innowise.db.PropertiesUtils;
import by.innowise.exception.MigrationException;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import static by.innowise.migrations.MigrationExecutor.applyMigration;
import static by.innowise.migrations.MigrationExecutor.ensureLockTableExists;
import static by.innowise.migrations.MigrationExecutor.isDatabaseLocked;
import static by.innowise.migrations.MigrationExecutor.lockDatabase;
import static by.innowise.migrations.MigrationExecutor.unlockDatabase;

/**
 * Класс для работы с примененными и новыми миграциями
 */
@Slf4j
public class MigrationManager {

    public static final String DIRECTORY_PATH = "migrations/";
    public static final String VERSION = "version";
    public static final String DB_USERNAME = "db.username";
    public static final String MSG = "База данных уже заблокирована другим процессом. Миграция невозможна";

    private MigrationManager() {
    }

    /**
     * Метод, который выполняет не примененные миграции к БД
     */
    public static void migrate() {
        try (Connection connection = ConnectionManager.getConnection()) {
            connection.setAutoCommit(false);
            ensureLockTableExists(connection);
            if (isDatabaseLocked(connection)) {
                log.warn(MSG);
                return;
            }
            lockDatabase(connection, PropertiesUtils.getProperty(DB_USERNAME));
            ensureHistoryTableExists(connection);
            List<File> migrationFiles = MigrationFileReader.getMigrationFiles(DIRECTORY_PATH);
            for (File file : migrationFiles) {
                if (!isMigrationApplied(connection, file)) {
                    applyMigration(connection, file);
                }
            }
            unlockDatabase(connection);
            connection.commit();
        } catch (Exception e) {
            log.error("Ошибка во время миграции.", e);
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
            ensureLockTableExists(connection);
            if (isDatabaseLocked(connection)) {
                log.warn(MSG);
                return;
            }
            lockDatabase(connection, PropertiesUtils.getProperty((DB_USERNAME)));
            clearDatabase(connection);
            ensureHistoryTableExists(connection);
            List<File> migrationFiles = MigrationFileReader.getMigrationFiles(DIRECTORY_PATH);
            for (File file : migrationFiles) {
                String version = MigrationFileReader.extractVersion(file);
                if (version.compareTo(tag) > 0) {
                    log.info("Достигнута указанная версия {}. Остановка выполнения миграций.", tag);
                    break;
                }
                applyMigration(connection, file);
            }
            markMigrationsAsRevertedAfterTag(connection, tag);
            unlockDatabase(connection);
            connection.commit();
            log.info("Откат до версии {} успешно выполнен.", tag);
        } catch (Exception e) {
            log.error("Ошибка при выполнении отката до версии.", e);
        }
    }

    /**
     * Метод, который отменяет все миграции после date
     *
     * @param date - дата, к которой необходимо откатить БД
     */
    public static void rollbackToDate(String date) {
        try (Connection connection = ConnectionManager.getConnection()) {
            connection.setAutoCommit(false);
            ensureLockTableExists(connection);
            if (isDatabaseLocked(connection)) {
                log.warn(MSG);
                return;
            }
            lockDatabase(connection, PropertiesUtils.getProperty((DB_USERNAME)));
            Timestamp rollbackTimestamp = parseDateToTimestamp(date);
            clearDatabase(connection);
            ensureHistoryTableExists(connection);
            List<File> migrationFiles = MigrationFileReader.getMigrationFiles(DIRECTORY_PATH);
            for (File file : migrationFiles) {
                String version = MigrationFileReader.extractVersion(file);
                String appliedAtQuery = "SELECT applied_at FROM migration_history WHERE version = ? AND reverted = FALSE";
                applyMigrations(file, connection, appliedAtQuery, version, rollbackTimestamp);
            }
            String updateQuery = """
                        UPDATE migration_history
                        SET reverted = TRUE
                        WHERE applied_at > ?;
                    """;
            try (PreparedStatement ps = connection.prepareStatement(updateQuery)) {
                ps.setTimestamp(1, rollbackTimestamp);
                int updatedRows = ps.executeUpdate();
                if (updatedRows > 0) {
                    log.info("Откат миграций, выполненных после даты {}", rollbackTimestamp);
                } else {
                    log.info("Миграций после даты {} не найдено.", rollbackTimestamp);
                }
            }
            unlockDatabase(connection);
            connection.commit();
        } catch (SQLException e) {
            log.error("Ошибка при выполнении отката до даты.", e);
        }
    }

    /**
     * Метод, проводящий откат БД на заданное количество миграций
     *
     * @param count - количество миграций, на которое надо откатить
     */
    public static void rollbackCount(int count) {
        try (Connection connection = ConnectionManager.getConnection()) {
            connection.setAutoCommit(false);
            ensureLockTableExists(connection);
            if (isDatabaseLocked(connection)) {
                log.warn("База данных уже заблокирована другим процессом. Миграция невозможна.");
                return;
            }
            lockDatabase(connection, PropertiesUtils.getProperty((DB_USERNAME)));
            String selectQuery = """
                        SELECT id, version
                        FROM migration_history
                        WHERE reverted = FALSE
                        ORDER BY version DESC
                        LIMIT ?;
                    """;
            try (PreparedStatement ps = connection.prepareStatement(selectQuery)) {
                ps.setInt(1, count);
                try (ResultSet rs = ps.executeQuery()) {
                    List<String> versionsToRollback = new ArrayList<>();
                    while (rs.next()) {
                        versionsToRollback.add(rs.getString(VERSION));
                    }
                    if (versionsToRollback.isEmpty()) {
                        log.info("Нет миграций для отката");
                        return;
                    }
                    clearDatabase(connection);
                    ensureHistoryTableExists(connection);
                    List<File> migrationFiles = MigrationFileReader.getMigrationFiles(DIRECTORY_PATH);
                    for (File file : migrationFiles) {
                        String version = MigrationFileReader.extractVersion(file);
                        if (!versionsToRollback.contains(version) && isMigrationApplied(connection, file)) {
                            applyMigration(connection, file);
                        }
                    }
                    markMigrationsAsReverted(connection, versionsToRollback);
                }
            }
            unlockDatabase(connection);
            connection.commit();
            log.info("Откат последних {} миграций успешно выполнен.", count);
        } catch (Exception e) {
            log.error("Ошибка при выполнении отката последних миграций.", e);
        }
    }

    /**
     * Метод, выводящий информацию о текущем состоянии БД, включая версию базы данных и список всех миграций
     */
    public static void info() {
        try (Connection connection = ConnectionManager.getConnection()) {
            ensureLockTableExists(connection);
            if (isDatabaseLocked(connection)) {
                log.warn("База данных уже заблокирована другим процессом. Миграция невозможна.");
                return;
            }
            lockDatabase(connection, PropertiesUtils.getProperty((DB_USERNAME)));
            log.info("Получение статуса базы данных...");
            String currentVersionQuery = """
                        SELECT version FROM migration_history
                        WHERE reverted = FALSE
                        ORDER BY version DESC LIMIT 1
                    """;
            try (PreparedStatement ps = connection.prepareStatement(currentVersionQuery)) {
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    String currentVersion = rs.getString(VERSION);
                    log.info("Актуальная версия базы данных: {}", currentVersion);
                } else {
                    log.info("Миграции не применялись. База данных находится в начальном состоянии.");
                }
            }
            String allMigrationsQuery = """
                        SELECT version, description, applied_at, reverted FROM migration_history
                        WHERE reverted = FALSE
                        ORDER BY applied_at
                    """;
            try (PreparedStatement ps = connection.prepareStatement(allMigrationsQuery)) {
                ResultSet rs = ps.executeQuery();
                log.info("Список миграций:");
                while (rs.next()) {
                    String version = rs.getString(VERSION);
                    String description = rs.getString("description");
                    String appliedAt = rs.getTimestamp("applied_at").toString();
                    boolean reverted = rs.getBoolean("reverted");

                    log.info("  - Версия: {}, Описание: {}, Применена: {}, Откатана: {}",
                            version, description, appliedAt, reverted ? "Да" : "Нет");
                }
                unlockDatabase(connection);
            }
        } catch (SQLException e) {
            log.error("Ошибка при получении статуса базы данных.", e);
        }
    }


    private static void markMigrationsAsReverted(Connection connection, List<String> versions) throws SQLException {
        String updateQuery = "UPDATE migration_history SET reverted = TRUE WHERE version = ?";
        try (PreparedStatement ps = connection.prepareStatement(updateQuery)) {
            for (String version : versions) {
                ps.setString(1, version);
                ps.addBatch();
            }
            int[] updateCounts = ps.executeBatch();
            log.info("Помечено как откатанные: {} миграций.", updateCounts.length);
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
            log.info("Таблица 'migration_history' проверена или успешно создана.");
        } catch (SQLException e) {
            log.error("Ошибка при создании таблицы 'migration_history'.", e);
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

    private static void applyMigrations(File file, Connection connection, String appliedAtQuery, String version, Timestamp rollbackTimestamp) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(appliedAtQuery)) {
            ps.setString(1, version);
            ResultSet rs = ps.executeQuery();
            if (rs.next() && rs.getTimestamp("applied_at").before(rollbackTimestamp)) {
                applyMigration(connection, file);
            }
        } catch (IOException e) {
            throw new MigrationException("Что-то не так с выполнением миграций...");
        }
    }

    private static void markMigrationsAsRevertedAfterTag(Connection connection, String tag) throws SQLException {
        String updateQuery = "UPDATE migration_history SET reverted = TRUE WHERE version > ?";
        try (PreparedStatement ps = connection.prepareStatement(updateQuery)) {
            ps.setString(1, tag);
            ps.executeUpdate();
            log.info("Миграции после версии {} помечены как откатанные.", tag);
        }
    }

    private static Timestamp parseDateToTimestamp(String date) {
        try {
            if (date.contains("T")) {
                date = date.replace("T", " ");
            }
            if (date.matches("\\d{4}-\\d{2}-\\d{2}")) {
                date += " 00:00:00";
            }

            return Timestamp.valueOf(date);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Неверный формат даты, используйте формат 'yyyy-MM-dd HH:mm:ss' или 'yyyy-MM-dd'.", e);
        }
    }

    private static void clearDatabase(Connection connection) {
        log.info("Очистка базы данных...");
        try (Statement stmt = connection.createStatement()) {
            String dropTablesSQL = """
                        DO $$
                        DECLARE
                            r RECORD;
                        BEGIN
                            FOR r IN (SELECT tablename FROM pg_tables WHERE schemaname = 'public' AND tablename != 'migration_history' AND tablename != 'migration_lock') LOOP
                                EXECUTE 'DROP TABLE IF EXISTS ' || r.tablename || ' CASCADE';
                            END LOOP;
                        END $$;
                    """;
            stmt.execute(dropTablesSQL);
            log.info("Все таблицы, кроме 'migration_history' и 'migration_lock', успешно удалены.");
        } catch (SQLException e) {
            log.error("Ошибка при очистке базы данных.", e);
        }
    }
}
