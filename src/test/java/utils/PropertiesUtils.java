package utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertiesUtils {

    private static final Properties properties = new Properties();

    static {
        try (InputStream input = getPropertiesFile()) {
            if (input == null) {
                throw new IOException("Файл конфигурации не найден");
            }
            properties.load(input);
        } catch (IOException e) {
            throw new RuntimeException("Ошибка при загрузке файла конфигурации", e);
        }
    }

    private static InputStream getPropertiesFile() {
        String env = System.getProperty("env");
        if ("test".equalsIgnoreCase(env)) {
            return PropertiesUtils.class.getClassLoader().getResourceAsStream("application-test.properties");
        }
        return PropertiesUtils.class.getClassLoader().getResourceAsStream("application.properties");
    }

    public static String getProperty(String key) {
        return properties.getProperty(key);
    }
}
