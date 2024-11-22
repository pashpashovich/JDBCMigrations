package by.innowise.exception;

/**
 * Класс исключения, связанное с загрузкой properties из application.properties
 */
public class PropertiesException extends RuntimeException {
    public PropertiesException(String message) {
        super(message);
    }
}
