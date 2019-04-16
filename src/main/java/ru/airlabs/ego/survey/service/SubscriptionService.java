package ru.airlabs.ego.survey.service;

/**
 * Интерфейс сервиса для работы с подписками
 *
 * @author Roman Kochergin
 */
public interface SubscriptionService {

    /**
     * Подписаться
     *
     * @param eventId идентификатор события
     * @param userId  идентификатор пользователя
     * @param email   имейл
     */
    void subscribe(Long eventId, Long userId, String email);

    /**
     * Отписаться
     *
     * @param eventId идентификатор события
     * @param userId  идентификатор пользователя
     * @param email   имейл
     */
    void unsubscribe(Long eventId, Long userId, String email);

    /**
     * Согласие или отказ на приглашение по вакансии
     *
     * @param vacancyId идентификатор вакансии
     * @param userId    идентификатор пользователя
     * @param response  состояние приглашения пользователя
     */
    void setInvitationResponse(Long vacancyId, Long userId, Integer response);
}
