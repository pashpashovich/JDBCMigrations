package by.innowise.migrations;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Класс для выполнения SQL-запросов и блокировки БД
 */
@Slf4j
public class MigrationExecutor {

    private MigrationExecutor() {
    }

    /**
     * Метод, который выполняет одну миграцию, определенную в File
     *
     * @param connection - соединение к БД
     * @param file       - файл с SQL скриптом миграции
     * @throws SQLException - ошибка, связанная с доступом или со взаимодействием БД
     * @throws IOException  - ошибка чтения/записи
     */
    public static void applyMigration(Connection connection, File file) throws SQLException, IOException {
        String sql = MigrationFileReader.readSQL(file);
        long startTime = System.currentTimeMillis();
        MigrationExecutor.executeQuery(connection, sql);
        long executionTime = System.currentTimeMillis() - startTime;
        String insertHistorySQL = """
                    INSERT INTO migration_history (version, description, script, checksum, execution_time, success, reverted, applied_at)
                    VALUES (?, ?, ?, ?, ?, ?, FALSE, CURRENT_TIMESTAMP)
                    ON CONFLICT (version) DO UPDATE SET
                        description = EXCLUDED.description,
                        script = EXCLUDED.script,
                        checksum = EXCLUDED.checksum,
                        execution_time = EXCLUDED.execution_time,
                        success = EXCLUDED.success,
                        reverted = FALSE,
                        applied_at = CASE WHEN migration_history.reverted = TRUE THEN CURRENT_TIMESTAMP ELSE migration_history.applied_at END;
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

    /**
     * Метод, который проверяет, что таблица для отслеживания блокировок БД существует, иначе создает ее
     *
     * @param connection - подключение к БД
     * @throws SQLException - ошибка, связанная с доступом или со взаимодействием БД
     */
    public static void ensureLockTableExists(Connection connection) throws SQLException {
        String createLockTableSQL = """
                    CREATE TABLE IF NOT EXISTS migration_lock (
                        id SERIAL PRIMARY KEY,
                        locked BOOLEAN NOT NULL DEFAULT FALSE,
                        locked_by VARCHAR(255),
                        locked_at TIMESTAMP
                    );
                """;
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createLockTableSQL);
            log.info("Таблица 'migration_lock' проверена или успешно создана.");
        }
    }

    /**
     * Метод, который проверяет, заблокирована ли БД в данный момент
     *
     * @param connection - подключение к БД
     * @return true, если заблокирована, false, если свободна
     * @throws SQLException - ошибка, связанная с доступом или со взаимодействием БД
     */
    public static boolean isDatabaseLocked(Connection connection) throws SQLException {
        String query = "SELECT locked FROM migration_lock WHERE id = 1";
        try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
            if (rs.next()) {
                return rs.getBoolean("locked");
            }
            return false;
        }
    }

    /**
     * Метод, который блокирует БД для других пользователей
     *
     * @param connection - подключение к БД
     * @param userName   - имя пользователя БД
     * @throws SQLException - ошибка, связанная с доступом или со взаимодействием БД
     */
    public static void lockDatabase(Connection connection, String userName) throws SQLException {
        String insertOrUpdateLockSQL = """
                    INSERT INTO migration_lock (id, locked, locked_by, locked_at)
                    VALUES (1, TRUE, ?, CURRENT_TIMESTAMP)
                    ON CONFLICT (id) DO UPDATE SET
                        locked = TRUE,
                        locked_by = EXCLUDED.locked_by,
                        locked_at = EXCLUDED.locked_at;
                """;
        try (PreparedStatement ps = connection.prepareStatement(insertOrUpdateLockSQL)) {
            ps.setString(1, userName);
            ps.executeUpdate();
            log.info("База данных успешно заблокирована пользователем: {}", userName);
        }
    }

    /**
     * Метод, который разблокирует БД для других пользователей
     *
     * @param connection - подключение к БД
     * @throws SQLException - ошибка, связанная с доступом или со взаимодействием БД
     */
    public static void unlockDatabase(Connection connection) throws SQLException {
        String unlockSQL = "UPDATE migration_lock SET locked = FALSE, locked_by = NULL, locked_at = NULL WHERE id = 1";
        try (PreparedStatement ps = connection.prepareStatement(unlockSQL)) {
            ps.executeUpdate();
            log.info("База данных успешно разблокирована.");
        }
    }

    /**
     * Выполняет один SQL запрос
     *
     * @param connection соединение к БД
     * @param sql        SQL запрос для выполнения
     * @throws SQLException если возникает ошибка во время выполнения
     */
    private static void executeQuery(Connection connection, String sql) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.executeUpdate();
        }
    }

}

