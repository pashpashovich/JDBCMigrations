package by.innowise.db;

import by.innowise.exception.NotFoundException;
import by.innowise.exception.PropertiesException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Класс для чтения конфигурации из application.properties
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PropertiesUtils {
    private static final Properties properties = new Properties();

    static {
        loadProperties();
    }

    /**
     * Загружает настройки из указанного файла или default файла application.properties
     */
    private static void loadProperties() {
        String configFile = System.getProperty("config.file", "application.properties");
        log.info("Загрузка конфигурации из файла: {}", configFile);
        try (InputStream input = PropertiesUtils.class.getClassLoader().getResourceAsStream(configFile)) {
            if (input == null) {
                throw new NotFoundException("Файл конфигурации не найден: " + configFile);
            }
            properties.load(input);
            log.info("Конфигурация успешно загружена из {}", configFile);
        } catch (IOException e) {
            log.error("Ошибка при загрузке файла конфигурации", e);
            throw new PropertiesException("Ошибка загрузки файла конфигурации: " + configFile);
        }
    }

    /**
     * Метод, возвращающий значение свойства по ключу
     *
     * @param key - ключ свойства
     * @return - значение свойства
     */
    public static String getProperty(String key) {
        return properties.getProperty(key);
    }
}
