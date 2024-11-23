package by.innowise.report;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Класс для генерации отчетов о миграциях
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MigrationReportGenerator {

    public static final String SELECT_VERSION_DESCRIPTION_SUCCESS_REVERTED_APPLIED_AT_FROM_MIGRATION_HISTORY_ORDER_BY_APPLIED_AT = "SELECT version, description, success, reverted, applied_at FROM migration_history ORDER BY applied_at";
    public static final String SELECT_VERSION_DESCRIPTION_SUCCESS_REVERTED_APPLIED_AT_FROM_MIGRATION_HISTORY_ORDER_BY_APPLIED_AT1 = "SELECT version, description, success, reverted, applied_at FROM migration_history ORDER BY applied_at";
    private static final String REPORTS_DIRECTORY = "reports";

    /**
     * Генерирует отчет в формате CSV.
     *
     * @param connection - соединение с базой данных
     * @throws SQLException - ошибка при выполнении SQL-запроса
     * @throws IOException  - ошибка при записи файла
     */
    public static void generateCsvReport(Connection connection) throws SQLException, IOException {
        ensureReportsDirectoryExists();
        Path csvPath = Paths.get(REPORTS_DIRECTORY, "migration_report.csv");
        try (PreparedStatement ps = connection.prepareStatement(SELECT_VERSION_DESCRIPTION_SUCCESS_REVERTED_APPLIED_AT_FROM_MIGRATION_HISTORY_ORDER_BY_APPLIED_AT);
             ResultSet rs = ps.executeQuery();
             FileWriter writer = new FileWriter(csvPath.toFile())) {
            writer.append("Version,Description,Success,Reverted,Applied At\n");
            while (rs.next()) {
                writer.append(rs.getString("version")).append(",")
                        .append(rs.getString("description")).append(",")
                        .append(String.valueOf(rs.getBoolean("success"))).append(",")
                        .append(String.valueOf(rs.getBoolean("reverted"))).append(",")
                        .append(rs.getTimestamp("applied_at").toString()).append("\n");
            }
        }
        log.info("CSV отчет о миграциях успешно создан: {}", csvPath.toAbsolutePath());
    }

    /**
     * Генерирует отчет в формате JSON.
     *
     * @param connection - соединение с базой данных
     * @throws SQLException - ошибка при выполнении SQL-запроса
     * @throws IOException  - ошибка при записи файла
     */
    public static void generateJsonReport(Connection connection) throws SQLException, IOException {
        ensureReportsDirectoryExists();
        Path jsonPath = Paths.get(REPORTS_DIRECTORY, "migration_report.json");

        List<MigrationReportEntry> reportEntries = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(SELECT_VERSION_DESCRIPTION_SUCCESS_REVERTED_APPLIED_AT_FROM_MIGRATION_HISTORY_ORDER_BY_APPLIED_AT1);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                reportEntries.add(new MigrationReportEntry(
                        rs.getString("version"),
                        rs.getString("description"),
                        rs.getBoolean("success"),
                        rs.getBoolean("reverted"),
                        rs.getTimestamp("applied_at").toString()
                ));
            }
        }

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.writeValue(jsonPath.toFile(), reportEntries);

        log.info("JSON отчет о миграциях успешно создан: {}", jsonPath.toAbsolutePath());
    }

    /**
     * Проверяет, существует ли директория для отчетов, и создает ее, если не существует
     *
     * @throws IOException - ошибка при создании директории
     */
    private static void ensureReportsDirectoryExists() throws IOException {
        Path reportsPath = Paths.get(REPORTS_DIRECTORY);
        if (!Files.exists(reportsPath)) {
            Files.createDirectories(reportsPath);
            log.info("Директория для отчетов создана: {}", reportsPath.toAbsolutePath());
        }
    }

    /**
     * Класс для представления записи отчета.
     */
    @Getter
    @AllArgsConstructor
    private static class MigrationReportEntry {
        private final String version;
        private final String description;
        private final boolean success;
        private final boolean reverted;
        private final String appliedAt;
    }
}

