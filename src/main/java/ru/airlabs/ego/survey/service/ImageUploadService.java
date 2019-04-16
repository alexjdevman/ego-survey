package ru.airlabs.ego.survey.service;

import org.springframework.web.multipart.MultipartFile;
import ru.airlabs.ego.core.entity.QuestionAnswer;
import ru.airlabs.ego.survey.dto.FileContent;

import java.util.List;

/**
 * Интерфейс сервиса для загрузки изображений в приложении
 *
 * @author Aleksey Gorbachev
 */
public interface ImageUploadService {

    /**
     * Сохранение фотографии пользователя
     *
     * @param userId     идентификатор пользователя
     * @param questionId идентификатор вопроса
     * @param imageFile  файл, содержащий фотографию пользователя
     */
    void saveUserImage(Long userId, Long questionId, MultipartFile imageFile);

    /**
     * Получение списка ссылок на все загруженные фотографии пользователя
     *
     * @param userId    идентификатор пользователя
     * @param vacancyId идентификатор вакансии
     * @return список ссылок на загруженные фотографии
     */
    List<String> getImagesForUserAndVacancy(Long userId, Long vacancyId);

    /**
     * Получение содержимого фото пользователя в рамках вакансии и ответа на вопрос
     *
     * @param userId     идентификатор пользователя
     * @param vacancyId  идентификатор вакансии
     * @param questionId идентификатор вопроса
     * @return содержимое файла с фото пользователя
     */
    FileContent getUserImageContent(Long userId, Long vacancyId, Long questionId);

    /**
     * Проверка, загружена ли фотография для ответа на вопрос
     *
     * @param questionAnswer ответ на вопрос
     * @return true - если фото загружено, иначе - false
     */
    boolean isImageUploadForQuestionAnswer(QuestionAnswer questionAnswer);

    /**
     * Сохранение фото пользователя из файла резюме
     *
     * @param userId        идентификатор пользователя
     * @param imageContent  содержимое фото из резюме
     */
    void saveUserImageFromResume(Long userId, byte[] imageContent);

}
