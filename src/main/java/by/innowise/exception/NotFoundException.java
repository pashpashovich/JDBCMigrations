package by.innowise.exception;

/**
 * Класс исключения, связанное с невозможностью найти определенный ресурс
 */
public class NotFoundException extends RuntimeException{

    public NotFoundException(String message) {
        super(message);
    }
}
