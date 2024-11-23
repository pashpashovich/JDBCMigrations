package by.innowise.migrations;

import by.innowise.db.PropertiesUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Класс для выполнения SQL-запросов и блокировки БД с использованием pg_advisory_lock с тайм-аутом
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MigrationExecutor {
    private static final String INSERT_HISTORY_SQL = """
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
    private static final String UNLOCK_ALL_SQL = "SELECT pg_advisory_unlock_all()";
    /**
     * Уникальный идентификатор блокировки
     */
    private static final int LOCK_ID = 1;

    /**
     * Выполняет миграцию из указанного SQL-файла.
     *
     * @param connection - соединение к БД
     * @param file       - файл с SQL скриптом миграции
     * @throws SQLException - ошибка взаимодействия с БД
     * @throws IOException  - ошибка чтения файла
     */
    public static void applyMigration(Connection connection, File file) throws SQLException, IOException {
        String sql = MigrationFileReader.readSQL(file);
        long startTime = System.currentTimeMillis();
        executeQuery(connection, sql);
        long executionTime = System.currentTimeMillis() - startTime;
        try (PreparedStatement ps = connection.prepareStatement(INSERT_HISTORY_SQL)) {
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
     * Блокирует базу данных с тайм-аутом, если блокировка недоступна
     *
     * @param connection - подключение к БД
     * @throws SQLException - ошибка взаимодействия с БД
     */
    public static void lockDatabase(Connection connection) throws SQLException {
        String lockSql = "SELECT pg_try_advisory_lock(" + LOCK_ID + ")";
        long startTime = System.currentTimeMillis();
        while (true) {
            try (Statement stmt = connection.createStatement()) {
                boolean locked = stmt.executeQuery(lockSql).next() && stmt.getResultSet().getBoolean(1);
                if (locked) {
                    log.info("База данных успешно заблокирована");
                    return;
                }
                long elapsedTime = (System.currentTimeMillis() - startTime) / 1000;
                if (elapsedTime > Long.parseLong(PropertiesUtils.getProperty("migrations.lock_retry_timeout_s"))) {
                    log.warn("Не удалось получить блокировку за {} секунд. Принудительное снятие блокировки.", PropertiesUtils.getProperty("migrations.lock_retry_timeout_s"));
                    forceUnlockAll(connection);
                    continue;
                }
                log.info("Блокировка недоступна. Повтор через {} мс.", PropertiesUtils.getProperty("migrations.lock_retry_delay_ms"));
                Thread.sleep(Long.parseLong(PropertiesUtils.getProperty("migrations.lock_retry_delay_ms")));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new SQLException("Процесс блокировки был прерван", e);
            }
        }
    }

    /**
     * Снимает все блокировки текущего соединения.
     *
     * @param connection - подключение к БД
     * @throws SQLException - ошибка взаимодействия с БД
     */
    public static void forceUnlockAll(Connection connection) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(UNLOCK_ALL_SQL);
            log.info("Все блокировки текущего соединения успешно сняты");
        }
    }

    /**
     * Разблокирует базу данных.
     *
     * @param connection - подключение к БД
     * @throws SQLException - ошибка взаимодействия с БД
     */
    public static void unlockDatabase(Connection connection) throws SQLException {
        String unlockSql = "SELECT pg_advisory_unlock(" + LOCK_ID + ")";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(unlockSql);
            log.info("База данных успешно разблокирована.");
        }
    }

    /**
     * Выполняет один SQL запрос.
     *
     * @param connection - подключение к БД
     * @param sql        - SQL запрос
     * @throws SQLException - ошибка выполнения запроса
     */
    private static void executeQuery(Connection connection, String sql) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.executeUpdate();
        }
    }
}
