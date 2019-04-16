package ru.airlabs.ego.survey.exception;


/**
 * Исключение генерируемое при попытке зарегистрировать пользователя с уже имеющимся в базе email
 *
 * @author Aleksey Gorbachev
 */
public class EmailExistsException extends RuntimeException {

    public EmailExistsException(String message) {
        super(message);
    }
}
