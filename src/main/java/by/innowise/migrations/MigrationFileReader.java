package by.innowise.migrations;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Класс для поиска файлов миграций в ресурсах или внешних источниках
 */
public class MigrationFileReader {

    private MigrationFileReader() {
    }

    /**
     * Метод, которых находит файлы миграций
     *
     * @param directoryPath расположение папки с файлами миграций
     * @return возвращает список файлов с миграциями
     */
    public static List<File> getMigrationFiles(String directoryPath) {
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            File directory = new File(Objects.requireNonNull(classLoader.getResource(directoryPath)).toURI());
            if (!directory.exists() || !directory.isDirectory()) {
                throw new IllegalArgumentException("Каталог миграций не найден: " + directoryPath);
            }
            return Arrays.stream(Objects.requireNonNull(directory.listFiles()))
                    .filter(file -> file.getName().matches("V\\d+__.*\\.sql"))
                    .sorted(Comparator.comparing(MigrationFileReader::extractVersion))
                    .toList();
        } catch (URISyntaxException | NullPointerException e) {
            throw new IllegalArgumentException("Ошибка при доступе к директории миграций: " + directoryPath, e);
        }
    }

    /**
     * Метод, который определяет версию миграции по названию файла
     *
     * @param file сам файл миграции
     * @return возвращает значение версии в виде String
     */
    static String extractVersion(File file) {
        return file.getName().split("__")[0].substring(1);
    }

    /**
     * Метод, который возвращает строковое представление SQL-файла
     *
     * @param file - сам SQL файл
     * @return - содержимое SQL файла в виде String
     * @throws IOException - ошибка чтения файла
     */
    public static String readSQL(File file) throws IOException {
        return Files.readString(file.toPath());
    }

}
