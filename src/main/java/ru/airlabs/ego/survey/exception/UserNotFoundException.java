package ru.airlabs.ego.survey.exception;

/**
 * Исключение, генерируемое при попытке выполнить действия для несуществующего в системе пользователя
 *
 * @author Aleksey Gorbachev
 */
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(String message) {
        super(message);
    }
}
