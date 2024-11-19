package by.innowise.migrations;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class MigrationFileReader {
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
                    .collect(Collectors.toList());
        } catch (URISyntaxException | NullPointerException e) {
            throw new IllegalArgumentException("Ошибка при доступе к директории миграций: " + directoryPath, e);
        }
    }

    static String extractVersion(File file) {
        return file.getName().split("__")[0].substring(1);
    }

    public static String readSQL(File file) throws IOException {
        return Files.readString(file.toPath());
    }
}
