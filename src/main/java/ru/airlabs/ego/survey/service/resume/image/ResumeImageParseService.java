package ru.airlabs.ego.survey.service.resume.image;

import java.io.InputStream;

/**
 * Интерфейс сервиса для получения изображений пользователй из резюме
 *
 * @author Aleksey Gorbachev
 */
public interface ResumeImageParseService {

    /**
     * Получение содержимого изображения пользователя из резюме
     *
     * @param stream содержимое файла резюме (вх. поток)
     * @return байтовое содержимое изображения
     */
    byte[] getUserImageFromResume(InputStream stream);
}
