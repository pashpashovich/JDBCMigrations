package by.innowise.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Класс для управления подключениями к БД
 */
public class ConnectionManager {

    /**
     * Поле, представляющее собой отдельное подключение к БД
     */
    private static Connection connection;

    /**
     * Приватный конструктор
     */
    private ConnectionManager() {

    }

    /**
     * Метод, возвращающий объект типа подключение к БД
     * @return возвращает подключение к БД
     * @throws SQLException ошибка, связанная с доступом к БД
     */
    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            String url = PropertiesUtils.getProperty("db.url");
            String user = PropertiesUtils.getProperty("db.username");
            String password = PropertiesUtils.getProperty("db.password");
            connection = DriverManager.getConnection(url, user, password);
        }
        return connection;
    }
}
