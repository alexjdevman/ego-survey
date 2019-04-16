package ru.airlabs.ego.survey.service;

import ru.airlabs.ego.survey.dto.search.SearchUserResult;

import java.util.List;

/**
 * Интерфейс сервиса для реализации поиска в ЛК HR
 *
 * @author Aleksey Gorbachev
 */
public interface SearchService {

    /**
     * Выполнить поиск пользователей по строке запроса
     *
     * @param currentUserId идентификатор HR
     * @param searchQuery   строка поиска
     * @return список результатов поиска по пользователям
     */
    List<SearchUserResult> searchUsers(Long currentUserId, String searchQuery);
}
