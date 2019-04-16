package ru.airlabs.ego.survey.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.airlabs.ego.core.entity.User;
import ru.airlabs.ego.survey.dto.vacancy.VacancyUserDetail;

/**
 * Интерфейс сервиса для получения подробностей по вакансиям
 *
 * @author Aleksey Gorbachev
 */
public interface VacancyDetailService {

    /**
     * Получение откликов по вакансии
     *
     * @param vacancyId   идентификатор вакансии
     * @param currentUser аккаунт текущего пользователя HR
     * @param pageable    настройки пагинации и сортировки
     * @return постраничный сортированный список данных по откликам
     */
    Page<VacancyUserDetail> getFeedbackForVacancy(Long vacancyId, User currentUser, Pageable pageable);

    /**
     * Получение приглашений по вакансии
     *
     * @param vacancyId   идентификатор вакансии
     * @param currentUser аккаунт текущего пользователя HR
     * @param pageable    настройки пагинации и сортировки
     * @return постраничный сортированный список данных по приглашениям
     */
    Page<VacancyUserDetail> getInvitationsForVacancy(Long vacancyId, User currentUser, Pageable pageable);

    /**
     * Получение буфера приглашений по вакансии (приглашения с пустым способом отправки)
     *
     * @param vacancyId   идентификатор вакансии
     * @param currentUser аккаунт текущего пользователя HR
     * @param pageable    настройки пагинации и сортировки
     * @return постраничный сортированный список данных по приглашениям из буфера
     */
    Page<VacancyUserDetail> getBufferInvitationsForVacancy(Long vacancyId, User currentUser, Pageable pageable);

}
