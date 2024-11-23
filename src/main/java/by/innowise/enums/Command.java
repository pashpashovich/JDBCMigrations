package by.innowise.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Enum, представляющий собой возможные команды из CLI
 */
@Getter
@RequiredArgsConstructor
public enum Command {
    MIGRATE("migrate"),
    ROLLBACK("rollback"),
    ROLLBACK_TO_DATE("rollback-to-date"),
    ROLLBACK_COUNT("rollback-count"),
    INFO("info"),
    REPORT_JSON("report_json"),
    REPORT_CSV("report_csv");

    private final String commandName;

    /**
     * Метод, возвращающий enum из строкового представления команды
     * @param commandName - строковое представление команды
     * @return - команда типа enum
     */
    public static Command fromString(String commandName) {
        for (Command command : Command.values()) {
            if (command.commandName.equalsIgnoreCase(commandName)) {
                return command;
            }
        }
        throw new IllegalArgumentException("Неизвестная команда: " + commandName);
    }
}
