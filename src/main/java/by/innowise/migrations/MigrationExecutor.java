package by.innowise.migrations;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class MigrationExecutor {

    private MigrationExecutor() {
    }

    /**
     * Выполняет один SQL запрос
     *
     * @param connection соединение к БД
     * @param sql        SQL запрос для выполнения
     * @throws SQLException если возникает ошибка во время выполнения
     */
    public static void executeQuery(Connection connection, String sql) throws SQLException {
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.executeUpdate();
        }
    }

    /**
     * Выполняет несколько SQL запросов в одной транзакции
     *
     * @param connection соединение к БД
     * @param sqlQueries        SQL запросы для выполнения
     * @throws SQLException если возникает ошибка во время выполнения
     */
    public static void executeBatch(Connection connection, String[] sqlQueries) throws SQLException {
        connection.setAutoCommit(false);
        try {
            for (String query : sqlQueries) {
                try (PreparedStatement stmt = connection.prepareStatement(query)) {
                    stmt.executeUpdate();
                }
            }
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
        } finally {
            connection.setAutoCommit(true);
        }
    }
}

