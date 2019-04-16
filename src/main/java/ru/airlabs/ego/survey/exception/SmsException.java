package ru.airlabs.ego.survey.exception;

/**
 * Исключение, генерируемое при СМС-ошибках
 *
 * @author Roman Kochergin
 */
public class SmsException extends RuntimeException {

    public SmsException(String message) {
        super(message);
    }
}
