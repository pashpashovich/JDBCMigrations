package by.innowise;

import by.innowise.migrations.MigrationManager;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MigrationTool {
    public static void main(String[] args) {
        if (args.length == 0) {
            log.info("Команды: migrate, rollback,rollback-to-date, rollback-count, info");
            return;
        }
        try {
            switch (args[0].toLowerCase()) {
                case "migrate":
                    log.info("Запуск миграции...");
                    MigrationManager.migrate();
                    log.info("Миграции успешно применены.");
                    break;
                case "rollback":
                    if (args.length < 2) {
                        log.error("Укажите тег для отката (например, rollback <tag>).");
                        return;
                    }
                    String tag = args[1];
                    log.info("Откат миграций после тега: {}", tag);
                    MigrationManager.rollbackToTag(tag);
                    break;

                case "rollback-to-date":
                    if (args.length < 2) {
                        log.error("Укажите дату для отката (например, rollback-to-date YYYY-MM-DD).");
                        return;
                    }
                    String date = args[1];
                    log.info("Откат миграций до даты: {}", date);
                    MigrationManager.rollbackToDate(date);
                    break;

                case "rollback-count":
                    if (args.length < 2) {
                        log.error("Укажите количество миграций для отката (например, rollback-count <count>).");
                        return;
                    }
                    int count = Integer.parseInt(args[1]);
                    log.info("Откат последних {} миграций", count);
                    MigrationManager.rollbackCount(count);
                    break;
                case "info":
                    log.info("Проверка статуса базы данных...");
                    MigrationManager.info();
                    break;
                default:
                    log.warn("Неизвестная команда: {}", args[0]);
            }
        } catch (Exception e) {
            log.error("Произошла ошибка при выполнении команды.", e);
        }

    }
}
