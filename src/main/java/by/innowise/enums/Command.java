package by.innowise.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Command {
    MIGRATE("migrate"),
    ROLLBACK("rollback"),
    ROLLBACK_TO_DATE("rollback-to-date"),
    ROLLBACK_COUNT("rollback-count"),
    INFO("info");

    private final String commandName;

    public static Command fromString(String commandName) {
        for (Command command : Command.values()) {
            if (command.commandName.equalsIgnoreCase(commandName)) {
                return command;
            }
        }
        throw new IllegalArgumentException("Неизвестная команда: " + commandName);
    }
}
