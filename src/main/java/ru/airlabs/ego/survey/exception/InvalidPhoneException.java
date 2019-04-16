package ru.airlabs.ego.survey.exception;

/**
 * Исключение, генерируемое при неверном значении телефона
 *
 * @author Roman Kochergin
 */
public class InvalidPhoneException extends RuntimeException {

    public InvalidPhoneException(String message) {
        super(message);
    }
}
