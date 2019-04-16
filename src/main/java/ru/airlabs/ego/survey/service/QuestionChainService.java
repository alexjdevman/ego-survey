package ru.airlabs.ego.survey.service;

/**
 * Интерфейс сервиса для работы с цепочками вопросов
 *
 * @author Aleksey Gorbachev
 */
public interface QuestionChainService {

    /**
     * Формирование необходимых цепочек вопросов для опроса
     *
     * @param surveyId идентификатор опроса
     */
    void fillQuestionChainForSurvey(Long surveyId);
}
