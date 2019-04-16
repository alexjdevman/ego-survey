package ru.airlabs.ego.survey.exception;

/**
 * Исключение, генерируемое при ошибках загрузки фотографий пользователей
 *
 * @author Aleksey Gorbachev
 */
public class ImageUploadException extends RuntimeException {

    public ImageUploadException(String message) {
        super(message);
    }
}
