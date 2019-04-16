package ru.airlabs.ego.survey.exception;

/**
 * Исключение, генерируемое при неверном вводе каптчи
 *
 * @author Roman Kochergin
 */
public class InvalidCaptchaException extends RuntimeException {

    public InvalidCaptchaException(String message) {
        super(message);
    }
}
