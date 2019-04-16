package ru.airlabs.ego.survey.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.airlabs.ego.survey.dto.user.UserCompareResult;
import ru.airlabs.ego.survey.dto.user.UserGroupCompare;
import ru.airlabs.ego.survey.dto.vacancy.VacancyUserDetail;

import java.util.List;

/**
 * Интерфейс сервиса для работы с исследованиями
 *
 * @author Aleksey Gorbachev
 */
public interface ResearchService {

    /**
     * Получение списка участников исследования с процентом их совпадения
     *
     * @param researchId идентификатор исследования
     * @return список участников исследования
     */
    List<UserCompareResult> getUserCompareListForResearch(Long researchId);

    /**
     * Получение данных о сравнении групп пользователей с конкретным выбранным пользователем
     *
     * @param researchId идентификатор исследования
     * @param userId     идентификатор пользователя
     * @return данные о сравнении групп пользователей
     */
    List<UserGroupCompare> getGroupCompareWithUser(Long researchId, Long userId);

    /**
     * Получение данных о сравнении групп пользователей для категорийного исследования ("Поиск лидера")
     *
     * @param researchId    идентификатор исследования
     * @param currentUserId идентификатор пользователя проводящего исследование
     * @return данные о сравнении групп пользователей для категорийного исследования
     */
    List<UserGroupCompare> getGroupCompareForCategoryResearch(Long researchId, Long currentUserId);

    /**
     * Получение подробностей столбца сравнения в рамках конкретного диапазона совпадения
     *
     * @param researchId        идентификатор исследования
     * @param userId            идентификатор участника для сравнения
     * @param currentUserId     идентификатор пользователя проводящего исследование
     * @param chartColumnNumber номер блока для графика сравнения участников
     * @param pageable          настройки пагинации
     * @return список данных сравнения участников опроса с возможностью пагинации
     */
    Page<VacancyUserDetail> getGroupCompareUserDetails(Long researchId,
                                                       Long userId,
                                                       Long currentUserId,
                                                       Integer chartColumnNumber,
                                                       Pageable pageable);

    /**
     * Получение подробностей столбца сравнения групп пользователей для категорийного исследования ("Поиск лидера")
     *
     * @param researchId        идентификатор исследования
     * @param currentUserId     идентификатор пользователя проводящего исследование*
     * @param chartColumnNumber номер блока для графика сравнения участников
     * @param pageable          настройки пагинации
     * @return список данных с возможностью пагинации
     */
    Page<VacancyUserDetail> getGroupCompareCategoryDetails(Long researchId,
                                                           Long currentUserId,
                                                           Integer chartColumnNumber,
                                                           Pageable pageable);
}
