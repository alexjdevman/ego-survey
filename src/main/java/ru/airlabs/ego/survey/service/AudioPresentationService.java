package ru.airlabs.ego.survey.service;

/**
 * Интерфейс для работы с аудио-презентациями
 *
 * @author Aleksey Gorbachev
 */
public interface AudioPresentationService {

    /**
     * Путь к смикшированному файлу
     *
     * @param vacancyId - идентификатор вакансии
     * @return путь
     */
    String getMixPath(Long vacancyId);

    /**
     * Проверка, создан ли для вакансии аудио-стикер
     *
     * @param vacancyId - идентификатор вакансии
     * @return true/false
     */
    boolean isAudioPresentationExists(Long vacancyId);
}
