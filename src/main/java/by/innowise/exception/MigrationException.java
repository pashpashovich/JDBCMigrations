package by.innowise.exception;

/**
 * Класс исключения, связанный с применением миграций
 */
public class MigrationException extends RuntimeException{
    public MigrationException(String message) {
        super(message);
    }
}
