package by.innowise.db;

import by.innowise.exception.PropertiesException;
import by.innowise.exception.NotFoundException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Класс для чтения конфигурации из application.properties
 */
public class PropertiesUtils {
    /**
     * Подкласс Hashtable для загрузки свойств
     */
    private static final Properties properties = new Properties();

    private PropertiesUtils() {

    }


    static {
        try (InputStream input = PropertiesUtils.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (input == null) {
                throw new NotFoundException("Файл application.properties не найден в ресурсах!");
            }
            properties.load(input);
        } catch (IOException e) {
            throw new PropertiesException("Ошибка загрузки application.properties");
        }
    }

    /**
     * Метод, возвращающий значение свойства по ключу
     * @param key - ключ свойства
     * @return - значение свойства
     */
    public static String getProperty(String key) {
        return properties.getProperty(key);
    }
}
