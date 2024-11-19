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
            logger.info(args[0].toLowerCase());
            switch (args[0].toLowerCase()) {
                case "migrate":
                    logger.info("Запуск миграции...");
                    MigrationManager.migrate();
                    logger.info("Миграции успешно применены.");
                    break;
                case "rollback":
                    logger.info("Запуск отката...");
                    MigrationManager.rollback();
                    logger.info("Откат успешно выполнен.");
                    break;
                case "status":
                    logger.info("Проверка статуса базы данных...");
                    MigrationManager.status();
                    break;
                default:
                    logger.warn("Неизвестная команда: {}", args[0]);
            }
        } catch (Exception e) {
            logger.error("Произошла ошибка при выполнении команды.", e);
        }

    }
}
