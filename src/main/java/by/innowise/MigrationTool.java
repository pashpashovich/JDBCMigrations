package by.innowise;

import by.innowise.migrations.MigrationManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MigrationTool {
    private static final Logger logger = LoggerFactory.getLogger(MigrationTool.class);

    public static void main(String[] args) {
        if (args.length == 0) {
            logger.info("Команды: migrate, rollback, status");
            return;
        }
        try {
            switch (args[0].toLowerCase()) {
                case "migrate":
                    logger.info("Запуск миграции...");
                    MigrationManager.migrate();
                    logger.info("Миграции успешно применены.");
                    break;
                case "rollback":
                    if (args.length < 2) {
                        logger.error("Укажите тег для отката (например, rollback <tag>).");
                        return;
                    }
                    String tag = args[1];
                    logger.info("Откат миграций после тега: {}", tag);
                    MigrationManager.rollbackToTag(tag);
                    break;

                case "rollback-to-date":
                    if (args.length < 2) {
                        logger.error("Укажите дату для отката (например, rollback-to-date YYYY-MM-DD).");
                        return;
                    }
                    String date = args[1];
                    logger.info("Откат миграций до даты: {}", date);
                    MigrationManager.rollbackToDate(date);
                    break;

                case "rollback-count":
                    if (args.length < 2) {
                        logger.error("Укажите количество миграций для отката (например, rollback-count <count>).");
                        return;
                    }
                    int count = Integer.parseInt(args[1]);
                    logger.info("Откат последних {} миграций", count);
                    MigrationManager.rollbackCount(count);
                    break;
                case "status":
                    logger.info("Проверка статуса базы данных...");
                    MigrationManager.info();
                    break;
                default:
                    logger.warn("Неизвестная команда: {}", args[0]);
            }
        } catch (Exception e) {
            logger.error("Произошла ошибка при выполнении команды.", e);
        }

    }
}
