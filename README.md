# Задание
Библиотека для работы с миграциями в СУБД PostgreSQL
## Содержание 
- [Технологии](#технологии)
- [Инструкция по запуску](#инструкция-по-запуску)
- [Функционал](#Функционал)
- [Безопасность](#Безопасность)
## Технологии
- Java 21
- Gradle 8.5
- PostgreSQL
- Lombok
- JUnit
## Инструкция по запуску
1. Скачайте или склонируйте репозиторий.
2. Поменяйте значения в application.properties (application-test.properties для выполнения тестов) в зависимости от параметров вашей БД и ваших пожеланий:
```
db.url=jdbc:postgresql://localhost:5432/{Название вашей БД}
db.username={Имя пользователя}
db.password={Пароль}
migrations.dir={Относительный путь до папки с SQL файлами миграций}
migrations.lock_retry_delay_ms={Время в мс, через которые повторять попытки достучаться до заблокированной БД}
migrations.lock_retry_timeout_s={Время в с, после которого блокировка силой снимается с БД}
```
3. Соберите проект.
4. Можете выполнять команды через CLI, которые описаны в функционале.
## Функционал
Команды CLI:
- migrate
Применяются все миграции к БД, которые еще не были применены или были откатаны, лежащие в migrations.dir.
- rollback {номер миграции}
  Откатывается состояние БД до номера миграции, переданного вторым параметром. Например, было применено 5 миграций (1-5), тогда команда rollback 3 отменит миграции 4 и 5. 
- rollback-to-date {дата}
  Допустимые форматы даты: yyyy-MM-dd HH:mm:ss (с временем) или yyyy-MM-dd (без времени). Откатывается состояние БД до даты, переданной вторым параметром, то есть все миграции после переданной даты отменяются.
- rollback-count {число}
  Откатывает состояние БД на заданное количество примененных миграций, переданное вторым параметром. Например, было применено 5 миграций (1-5), тогда команда rollback-count 3 отменит миграции 5,4 и 3. 
- info 
  Выводит логгером информацию о текущем состоянии БД, а также информацию о всех миграциях. Пример,
```
Актуальная версия базы данных: 5
Список миграций:
Версия: 1, Описание: Migration V1__Create_users_table.sql, Применена: 2024-11-23 19:37:51.727471, Откатана: Нет
Версия: 2, Описание: Migration V2__Insert_into_users.sql, Применена: 2024-11-23 21:17:32.22781, Откатана: Нет
Версия: 3, Описание: Migration V3__Create_roles_table.sql, Применена: 2024-11-23 21:17:32.22781, Откатана: Нет
Версия: 4, Описание: Migration V4__Add_foreign_key_to_users.sql, Применена: 2024-11-23 21:17:32.22781, Откатана: Нет
Версия: 5, Описание: Migration V5__Insert_roles.sql, Применена: 2024-11-23 21:17:32.22781, Откатана: Нет
```
- report_json
  Формирует отчет о миграциях в формате JSON. Пример,
```
[ {
  "version" : "1",
  "description" : "Migration V1__Create_users_table.sql",
  "success" : true,
  "reverted" : false,
  "appliedAt" : "2024-11-23 19:17:20.873793"
}, {
  "version" : "2",
  "description" : "Migration V2__Insert_into_users.sql",
  "success" : true,
  "reverted" : false,
  "appliedAt" : "2024-11-23 19:17:20.873793"
}, {
  "version" : "3",
  "description" : "Migration V3__Create_roles_table.sql",
  "success" : true,
  "reverted" : false,
  "appliedAt" : "2024-11-23 19:17:20.873793"
}, {
  "version" : "4",
  "description" : "Migration V4__Add_foreign_key_to_users.sql",
  "success" : true,
  "reverted" : false,
  "appliedAt" : "2024-11-23 19:17:20.873793"
}, {
  "version" : "5",
  "description" : "Migration V5__Insert_roles.sql",
  "success" : true,
  "reverted" : false,
  "appliedAt" : "2024-11-23 19:17:20.873793"
} ]
```
- report_csv
  Формирует такой же отчет о миграциях только в формате CSV. 
## Безопасность
Реализован механизм блокировки для предотвращения конфликтов при работе с одной и той же БД несколькими пользователями с помощью pg_try_advisory_lock. Также учтено, что возможна ошибочная бесконечная блокировка БД одним пользователем, что решено с помощью силовой разблокировки БД через migrations.lock_retry_timeout_s, которое можно установить в application.properties
