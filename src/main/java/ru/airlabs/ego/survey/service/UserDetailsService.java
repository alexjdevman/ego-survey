package ru.airlabs.ego.survey.service;

import ru.airlabs.ego.survey.dto.user.UserDetail;
import ru.airlabs.ego.survey.dto.vacancy.VacancyUserDetail;

/**
 * Интерфейс сервиса для работы с данными пользователей
 *
 * @author Aleksey Gorbachev
 */
public interface UserDetailsService {

    /**
     * Получение карточки клиента
     *
     * @param userId           идентификатор клиента
     * @param vacancyId        идентификатор вакансии
     * @param currentManagerId идентификатор текущего авторизованного пользователя (HR)
     * @return данные по карточке клиента
     */
    VacancyUserDetail getUserDetailsByVacancy(Long userId, Long vacancyId, Long currentManagerId);

    /**
     * Редактирование данных пользователя
     *
     * @param userId        идентификатор пользователя
     * @param currentUserId идентификатор текущего пользователя
     * @param userDetail    данные из формы редактирования пользователя
     */
    void updateUserDetails(Long userId,
                           Long currentUserId,
                           UserDetail userDetail);

    /**
     * Удаление пользователя из вакансии или исследования
     *
     * @param userId    идентификатор пользователя
     * @param vacancyId идентификатор вакансии или исследования
     */
    void deleteUserFromVacancy(Long userId, Long vacancyId);
}
