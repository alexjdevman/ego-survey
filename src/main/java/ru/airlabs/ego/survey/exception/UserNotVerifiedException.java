package ru.airlabs.ego.survey.exception;

/**
 * Исключение, генерируемое при попытке авторизации не подтвержденного пользователя
 *
 * @author Aleksey Gorbachev
 */
public class UserNotVerifiedException extends RuntimeException {

    public UserNotVerifiedException(String message) {
        super(message);
    }
}
