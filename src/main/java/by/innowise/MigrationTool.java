package by.innowise;

import by.innowise.enums.Command;
import by.innowise.migrations.MigrationManager;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.sql.SQLException;

@Slf4j
public class MigrationTool {

    public static void main(String[] args) {
        if (args.length == 0) {
            log.info("Команды: migrate, rollback, rollback-to-date, rollback-count, info");
            return;
        }
        try {
            executeCommand(args);
        } catch (IllegalArgumentException e) {
            log.error("Некорректные аргументы команды: {}", e.getMessage());
        } catch (SQLException e) {
            log.error("Ошибка базы данных: {}", e.getMessage(), e);
        } catch (IOException e) {
            log.error("Ошибка ввода-вывода: {}", e.getMessage(), e);
        } catch (Exception e) {
            log.error("Неизвестная ошибка: {}", e.getMessage(), e);
        }
    }

    public static void executeCommand(String[] args) throws SQLException, IOException {
        Command command = Command.fromString(args[0]);
        switch (command) {
            case MIGRATE:
                executeWithLogging("migrate", MigrationManager::migrate);
                break;
            case ROLLBACK:
                String tag = getRequiredArgument(args, "Укажите тег для отката (например, rollback <tag>).");
                log.info("Откат миграций после тега: {}", tag);
                MigrationManager.rollbackToTag(tag);
                break;
            case ROLLBACK_TO_DATE:
                String date = getRequiredArgument(args, "Укажите дату для отката (например, rollback-to-date YYYY-MM-DD).");
                log.info("Откат миграций до даты: {}", date);
                MigrationManager.rollbackToDate(date);
                break;
            case ROLLBACK_COUNT:
                String countArg = getRequiredArgument(args, "Укажите количество миграций для отката (например, rollback-count <count>).");
                int count = parseInteger(countArg);
                log.info("Откат последних {} миграций", count);
                MigrationManager.rollbackCount(count);
                break;
            case INFO:
                executeWithLogging("info", MigrationManager::info);
                break;
        }
    }

    private static String getRequiredArgument(String[] args, String errorMessage) {
        if (args.length <= 1) {
            throw new IllegalArgumentException(errorMessage);
        }
        return args[1];
    }

    private static int parseInteger(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Количество миграций должно быть числом.");
        }
    }

    private static void executeWithLogging(String commandName, Runnable commandAction) {
        log.info("Выполнение команды: {}", commandName);
        try {
            commandAction.run();
            log.info("Команда '{}' успешно выполнена.", commandName);
        } catch (Exception e) {
            log.error("Ошибка при выполнении команды '{}': {}", commandName, e.getMessage(), e);
        }
    }
}
