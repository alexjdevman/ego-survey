package ru.airlabs.ego.survey.service;

import ru.airlabs.ego.core.entity.Survey;
import ru.airlabs.ego.core.entity.User;
import ru.airlabs.ego.survey.dto.survey.*;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Интерфейс сервиса для работы с опросами
 *
 * @author Aleksey Gorbachev
 */
public interface SurveyService {

    /**
     * Получение опроса по его идентификатору
     *
     * @param surveyId идентификатор опроса
     * @return опрос
     */
    Survey findById(Long surveyId);

    /**
     * Создание нового опроса
     *
     * @param name        имя опроса
     * @param currentUser текущий пользователь, создающий опрос
     * @param locale      текущая локаль
     * @return новый опрос
     */
    Survey addSurvey(String name, User currentUser, Locale locale);

    /**
     * Редактирование опроса
     *
     * @param surveyInfo данные по опросу
     * @param locale     текущая локаль
     * @return опрос
     */
    Survey updateSurvey(SurveyInfo surveyInfo, Locale locale);

    /**
     * Наполнение опроса категориями
     *
     * @param surveyId   идентификатор опроса
     * @param categories список категорий опроса
     */
    void fillSurveyWithCategories(Long surveyId, List<SurveyCategory> categories);

    /**
     * Получение данных по опросам, доступным пользователю (для передачи на UI)
     *
     * @param userId идентификатор пользователя
     * @param active признак активности опроса
     * @param locale текущая локаль
     * @return список данных по опросам
     */
    List<SurveyInfo> getAccessibleSurveyListForUser(Long userId, Boolean active, Locale locale);

    /**
     * Получить список неотвеченных вопросов
     *
     * @param userId   идентификатор пользователя
     * @param surveyId идентификатор опроса
     * @param locale   локаль
     * @param max      макс. кол-во вопросов
     * @return список
     */
    List<SurveyQuestion> getUnansweredSurveyQuestions(Long userId, Long surveyId, Locale locale, Integer max);

    /**
     * Получение количества оставшихся вопросов и общее время на интервью на старте
     *
     * @param userId   идентификатор пользователя
     * @param surveyId идентификатор опроса
     * @return количество оставшихся вопросов и общее время на интервью на старте
     */
    Map<String, Object> getRemainsQuestionsWithTime(Long userId, Long surveyId);

    /**
     * Получение количества оставшихся вопросов
     *
     * @param userId   идентификатор пользователя
     * @param surveyId идентификатор опроса
     * @return количество оставшихся вопросов
     */
    Integer getRemainsQuestionsCount(Long userId, Long surveyId);

    /**
     * Ответить на вопросы
     *
     * @param answers           ответы на вопросы
     * @param userSurveyStateId идентификатор состояния прохождения вопроса
     * @return результат прохождения пользователем опроса
     */
    SurveyResult answerSurveyQuestions(Collection<SurveyAnswer> answers, Long userSurveyStateId);

    /**
     * Получить список доступных категорий опросов
     *
     * @param locale текущая локаль
     * @return список категорий опросов
     */
    List<SurveyCategory> getSurveyCategories(Locale locale);

    /**
     * Получение данных по доступным опросам для пользователя
     *
     * @param userId идентификатор текущего пользователя
     * @param active признак активности опроса
     * @param locale текущая локаль
     * @return данные по доступным опросам
     */
    List<Map<String, Object>> getSurveyDataList(Long userId, Boolean active, Locale locale);

    /**
     * Определение, является ли опрос редактируемым
     * (можно ли наполнять и менять категории опроса)
     *
     * @param surveyId идентификатор опроса
     * @return true - если опрос редактируемый
     */
    boolean isSurveyEditable(Long surveyId);

    /**
     * Проверка, содержит ли опрос данный вопрос
     *
     * @param surveyId   идентификатор опроса
     * @param questionId идентификатор вопроса
     * @return true - если содержит, иначе false
     */
    boolean isSurveyContainsQuestion(Long surveyId, Long questionId);

    /**
     * Изменение статуса опроса
     *
     * @param surveyId идентификатор опроса
     */
    void changeSurveyState(Long surveyId);

    /**
     * Получение результата опроса пользователя
     * (на сколько % он соответствует эталону)
     *
     * @param userId   идентификатор пользователя
     * @param leaderId идентификатор эталонного сотрудника
     * @param surveyId идентификатор опроса
     * @return результат опроса пользователя (% схожести с эталоном)
     */
    Double compareUserWithLeaderBySurvey(Long userId, Long leaderId, Long surveyId);

    /**
     * Сохранение ошибки клиента при прохождении опроса
     *
     * @param userId      идентификатор пользователя, проходящего опрос
     * @param surveyId    идентификатор опроса
     * @param surveyError модель с текстом ошибки и юзерагентом
     */
    void saveSurveyError(Long userId, Long surveyId, SurveyError surveyError);

    /**
     * Сохранение данных по геолокации опроса
     *
     * @param surveyStateId идентификатор состояния прохождения опроса
     * @param location      данные по геолокации (широта + долгота)
     */
    void saveSurveyLocation(Long surveyStateId, SurveyStateLocation location);

    /**
     * Получение данных по глобальным параметрам пользователей (среднее время и глобальный % противоречий)
     *
     * @return глобальные параметры пользователей
     */
    GlobalUserScore getGlobalUserScore();

}
